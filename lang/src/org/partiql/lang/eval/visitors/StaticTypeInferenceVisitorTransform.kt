/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.TextElement
import com.amazon.ionelement.api.ionBool
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.StaticTypeMeta
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.ast.passes.inference.cast
import org.partiql.lang.ast.passes.inference.filterNullMissing
import org.partiql.lang.ast.passes.inference.isLob
import org.partiql.lang.ast.passes.inference.isNullOrMissing
import org.partiql.lang.ast.passes.inference.isNumeric
import org.partiql.lang.ast.passes.inference.isText
import org.partiql.lang.ast.passes.inference.isUnknown
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.staticType
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.errors.ProblemSeverity
import org.partiql.lang.errors.ProblemThrower
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.builtins.createBuiltinFunctionSignatures
import org.partiql.lang.eval.delegate
import org.partiql.lang.eval.getStartingSourceLocationMeta
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.BagType
import org.partiql.lang.types.BoolType
import org.partiql.lang.types.CollectionType
import org.partiql.lang.types.DecimalType
import org.partiql.lang.types.FloatType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.IntType
import org.partiql.lang.types.ListType
import org.partiql.lang.types.MissingType
import org.partiql.lang.types.NullType
import org.partiql.lang.types.NumberConstraint
import org.partiql.lang.types.SexpType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StringType
import org.partiql.lang.types.StructType
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.types.UnknownArguments
import org.partiql.lang.types.toTypedOpParameter
import org.partiql.lang.util.cartesianProduct
import org.partiql.pig.runtime.SymbolPrimitive

/**
 * A [PartiqlAst.VisitorTransform] that annotates nodes with their static type.
 *
 * Assumes [StaticTypeVisitorTransform] was run before this and all the implicit variables have been resolved.
 *
 * @param globalBindings The global bindings to the static environment.  This is data catalog purely from a lookup
 *                  perspective.
 * @param customFunctionSignatures Custom user-defined function signatures that can be called by the query.
 * @param customTypedOpParameters Mapping of custom type name to [TypedOpParameter] to be used for typed operators
 * (i.e CAST/IS).
 * @param problemHandler handles the semantic problems encountered through static type inference. Default handler will
 * throw on the first [SemanticException] with [ProblemSeverity.ERROR].
 */
internal class StaticTypeInferenceVisitorTransform(
    globalBindings: Bindings<StaticType>,
    customFunctionSignatures: List<FunctionSignature>,
    private val customTypedOpParameters: Map<String, TypedOpParameter>,
    private val problemHandler: ProblemHandler = ProblemThrower()
) : PartiqlAst.VisitorTransform() {

    /** Used to allow certain binding lookups to occur directly in the global scope. */
    private val globalTypeEnv = wrapBindings(globalBindings, 0)

    /** Captures a [StaticType] and the depth at which it is bound. */
    private data class TypeAndDepth(val type: StaticType, val depth: Int)

    /** Defines the current scope search order--i.e. globals first when in a FROM source, lexical everywhere else. */
    private enum class ScopeSearchOrder {
        LEXICAL,
        GLOBALS_THEN_LEXICAL
    }

    /** The built-in functions + the custom functions. */
    private val allFunctions: Map<String, FunctionSignature> =
        createBuiltinFunctionSignatures() + customFunctionSignatures.associateBy { it.name }

    /**
     * @param parentEnv the enclosing bindings
     * @param currentScopeDepth How deeply nested the current scope is.
     * - 0 means we are in the global scope
     * - 1 is the top-most statement with a `FROM` clause (i.e. select-from-where or DML operation),
     * - Values > 1 are for each subsequent level of nested sub-query.
     */
    private inner class VisitorTransform(
        private val parentEnv: Bindings<TypeAndDepth>,
        private val currentScopeDepth: Int
    ) : VisitorTransformBase() {

        /** Specifies the current scope search order--default is LEXICAL. */
        private var scopeOrder = ScopeSearchOrder.LEXICAL

        private val localsMap = mutableMapOf<String, StaticType>()

        private var localsOnlyEnv = wrapBindings(Bindings.ofMap(localsMap), currentScopeDepth)

        // because of the mutability of the above reference, we need to encode the lookup as a thunk
        private val currentEnv = Bindings.over { localsOnlyEnv[it] }.delegate(parentEnv)

        private fun PartiqlAst.Expr.withStaticType(type: StaticType) =
            this.withMeta(StaticTypeMeta.TAG, StaticTypeMeta(type)) as PartiqlAst.Expr

        private fun PartiqlAst.PathStep.withStaticType(type: StaticType) =
            this.withMeta(StaticTypeMeta.TAG, StaticTypeMeta(type)) as PartiqlAst.PathStep

        private fun PartiqlAst.Projection.withStaticType(type: StaticType) =
            this.withMeta(StaticTypeMeta.TAG, StaticTypeMeta(type)) as PartiqlAst.Projection

        private fun PartiqlAst.Expr.getStaticType(): StaticType =
            this.metas.staticType?.type ?: error("No inferred type information found on PartiqlAst: $this")

        private fun MetaContainer.getSourceLocation(): SourceLocationMeta =
            this[SourceLocationMeta.TAG] as SourceLocationMeta? ?: error("No source location found on PartiqlAst: $this")

        private fun handleDuplicateAliasesError(sourceLocationMeta: SourceLocationMeta) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocationMeta,
                    details = SemanticProblemDetails.DuplicateAliasesInSelectListItem
                )
            )
        }

        private fun handleNoSuchFunctionError(functionName: String, sourceLocationMeta: SourceLocationMeta) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocationMeta,
                    details = SemanticProblemDetails.NoSuchFunction(functionName)
                )
            )
        }

        private fun handleIncorrectNumberOfArgumentsToFunctionCallError(
            functionName: String,
            expectedArity: IntRange,
            actualArgCount: Int,
            sourceLocationMeta: SourceLocationMeta
        ) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocationMeta,
                    details = SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                        functionName,
                        expectedArity,
                        actualArgCount
                    )
                )
            )
        }

        // TODO: https://github.com/partiql/partiql-lang-kotlin/issues/508 consider not working directly with strings for `op`
        private fun handleIncompatibleDataTypesForOpError(actualTypes: List<StaticType>, op: String, sourceLocationMeta: SourceLocationMeta) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocationMeta,
                    details = SemanticProblemDetails.IncompatibleDatatypesForOp(
                        actualTypes,
                        op
                    )
                )
            )
        }

        private fun handleIncompatibleDataTypeForExprError(expectedType: StaticType, actualType: StaticType, sourceLocationMeta: SourceLocationMeta) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocationMeta,
                    details = SemanticProblemDetails.IncompatibleDataTypeForExpr(expectedType, actualType)
                )
            )
        }

        private fun handleExpressionAlwaysReturnsNullOrMissingError(sourceLocationMeta: SourceLocationMeta) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocationMeta,
                    details = SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing
                )
            )
        }

        private fun handleNullOrMissingFunctionArgument(functionName: String, sourceLocationMeta: SourceLocationMeta) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocationMeta,
                    details = SemanticProblemDetails.NullOrMissingFunctionArgument(
                        functionName = functionName
                    )
                )
            )
        }

        private fun handleInvalidArgumentTypeForFunction(functionName: String, expectedType: StaticType, actualType: StaticType, sourceLocationMeta: SourceLocationMeta) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocationMeta,
                    details = SemanticProblemDetails.InvalidArgumentTypeForFunction(
                        functionName = functionName,
                        expectedType = expectedType,
                        actualType = actualType
                    )
                )
            )
        }

        private fun addLocal(name: String, type: StaticType) {
            val existing = localsOnlyEnv[BindingName(name, BindingCase.INSENSITIVE)]
            if (existing != null) {
                TODO(
                    "A variable named '$name' was already defined in this scope. " +
                        "This wouldn't be the case if StaticTypeVisitorTransform was executed first."
                )
            }
            localsMap[name] = type
            // this requires a new instance because of how [Bindings.ofMap] works
            localsOnlyEnv = wrapBindings(Bindings.ofMap(localsMap), currentScopeDepth)
        }

        private fun Bindings<TypeAndDepth>.lookupBinding(bindingName: BindingName): StaticType? = this[bindingName]?.type

        /**
         * Encapsulates variable reference lookup, layering the scoping
         * rules from the exclusions given the current state.
         *
         * Returns an instance of [StaticType] if the binding was found, otherwise returns null.
         */
        private fun findBind(bindingName: BindingName, scopeQualifier: PartiqlAst.ScopeQualifier): StaticType? {
            // Override the current scope search order if the var is lexically qualified.
            val overriddenScopeSearchOrder = when (scopeQualifier) {
                is PartiqlAst.ScopeQualifier.LocalsFirst -> ScopeSearchOrder.LEXICAL
                is PartiqlAst.ScopeQualifier.Unqualified -> this.scopeOrder
            }
            val scopes: List<Bindings<TypeAndDepth>> = when (overriddenScopeSearchOrder) {
                ScopeSearchOrder.GLOBALS_THEN_LEXICAL -> listOf(globalTypeEnv, currentEnv)
                ScopeSearchOrder.LEXICAL -> listOf(currentEnv, globalTypeEnv)
            }

            return scopes
                .asSequence()
                .mapNotNull { it.lookupBinding(bindingName) }
                .firstOrNull()
        }

        override fun transformExprId(node: PartiqlAst.Expr.Id): PartiqlAst.Expr {
            val bindingName = BindingName(node.name.text, node.case.toBindingCase())

            val foundType = findBind(bindingName, node.qualifier) ?: error(
                "No such variable named ${node.name.text}. " +
                    "This wouldn't be the case if StaticTypeVisitorTransform was executed first."
            )

            return node.withStaticType(foundType)
        }

        private fun transformNAry(
            expr: PartiqlAst.Expr,
            operands: List<PartiqlAst.Expr>,
            compute: (List<StaticType>) -> StaticType
        ): PartiqlAst.Expr {
            val argTypes = operands.map { it.getStaticType() }
            val inferredType = compute(argTypes)
            return expr.withStaticType(inferredType)
        }

        // Arithmetic NAry ops: ADD, SUB, MUL, DIV, MOD
        override fun transformExprPlus(node: PartiqlAst.Expr.Plus): PartiqlAst.Expr {
            val nAry = super.transformExprPlus(node) as PartiqlAst.Expr.Plus
            val type = when {
                nAry.operands.size < 2 -> throw IllegalArgumentException("PartiqlAst.Expr.Plus must have at least 2 arguments")
                else -> computeReturnTypeForArithmeticNAry(nAry, nAry.operands, "+")
            }
            return nAry.withStaticType(type)
        }

        override fun transformExprPos(node: PartiqlAst.Expr.Pos): PartiqlAst.Expr {
            val nAry = super.transformExprPos(node) as PartiqlAst.Expr.Pos
            val type = computeReturnTypeForArithmeticUnary(nAry, listOf(nAry.expr), "+")
            return nAry.withStaticType(type)
        }

        override fun transformExprMinus(node: PartiqlAst.Expr.Minus): PartiqlAst.Expr {
            val nAry = (super.transformExprMinus(node) as PartiqlAst.Expr.Minus)
            val type = when {
                nAry.operands.size < 2 -> throw IllegalArgumentException("PartiqlAst.Expr.Minus must have at least 2 arguments")
                else -> computeReturnTypeForArithmeticNAry(nAry, nAry.operands, "-")
            }
            return nAry.withStaticType(type)
        }

        override fun transformExprNeg(node: PartiqlAst.Expr.Neg): PartiqlAst.Expr {
            val nAry = super.transformExprNeg(node) as PartiqlAst.Expr.Neg
            val type = computeReturnTypeForArithmeticUnary(nAry, listOf(nAry.expr), "-")
            return nAry.withStaticType(type)
        }

        override fun transformExprTimes(node: PartiqlAst.Expr.Times): PartiqlAst.Expr {
            val nAry = (super.transformExprTimes(node) as PartiqlAst.Expr.Times)
            return nAry.withStaticType(computeReturnTypeForArithmeticNAry(nAry, nAry.operands, "*"))
        }

        override fun transformExprDivide(node: PartiqlAst.Expr.Divide): PartiqlAst.Expr {
            val nAry = (super.transformExprDivide(node) as PartiqlAst.Expr.Divide)
            return nAry.withStaticType(computeReturnTypeForArithmeticNAry(nAry, nAry.operands, "/"))
        }

        override fun transformExprModulo(node: PartiqlAst.Expr.Modulo): PartiqlAst.Expr {
            val nAry = (super.transformExprModulo(node) as PartiqlAst.Expr.Modulo)
            return nAry.withStaticType(computeReturnTypeForArithmeticNAry(nAry, nAry.operands, "%"))
        }

        /**
         * Returns true if all of the provided [operands] are comparable to each other and are not unknown. Otherwise,
         * returns false.
         *
         * If an operand is not comparable to another, the [SemanticProblemDetails.IncompatibleDatatypesForOp] error is
         * handled by [problemHandler]. If an operand is unknown, the
         * [SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing] error is handled by [problemHandler].
         *
         * TODO: consider if collection comparison semantics should be different (e.g. errors over warnings,
         *  more details in error message): https://github.com/partiql/partiql-lang-kotlin/issues/505
         */
        private fun operandsAreComparable(operands: List<PartiqlAst.Expr>, op: String, metas: MetaContainer): Boolean {
            val operandsTypes = operands.map { it.getStaticType() }
            var hasValidOperands = true

            // check for comparability of all operands. currently only adds one data type mismatch error
            outerLoop@ for (i in operandsTypes.indices) {
                for (j in i + 1 until operandsTypes.size) {
                    if (!operandsTypes[i].isComparableTo(operandsTypes[j])) {
                        handleIncompatibleDataTypesForOpError(operandsTypes, op, metas.getSourceLocation())
                        hasValidOperands = false
                        break@outerLoop
                    }
                }
            }

            // check for an unknown operand type
            if (operandsTypes.any { operand -> operand.isUnknown() }) {
                handleExpressionAlwaysReturnsNullOrMissingError(metas.getSourceLocation())
                hasValidOperands = false
            }
            return hasValidOperands
        }

        // Compare NAry ops: EQ, NE, GT, GTE, LT, LTE, BETWEEN
        override fun transformExprEq(node: PartiqlAst.Expr.Eq): PartiqlAst.Expr {
            val nAry = super.transformExprEq(node) as PartiqlAst.Expr.Eq
            return if (operandsAreComparable(nAry.operands, "=", nAry.metas)) {
                transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, it, ::getTypeForNAryCompareOperations) }
            } else {
                return nAry.withStaticType(StaticType.BOOL)
            }
        }

        override fun transformExprNe(node: PartiqlAst.Expr.Ne): PartiqlAst.Expr {
            val nAry = super.transformExprNe(node) as PartiqlAst.Expr.Ne
            return if (operandsAreComparable(nAry.operands, "!=", nAry.metas)) {
                transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, it, ::getTypeForNAryCompareOperations) }
            } else {
                return nAry.withStaticType(StaticType.BOOL)
            }
        }

        override fun transformExprGt(node: PartiqlAst.Expr.Gt): PartiqlAst.Expr {
            val nAry = super.transformExprGt(node) as PartiqlAst.Expr.Gt
            return if (operandsAreComparable(nAry.operands, ">", nAry.metas)) {
                transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, it, ::getTypeForNAryCompareOperations) }
            } else {
                return nAry.withStaticType(StaticType.BOOL)
            }
        }

        override fun transformExprGte(node: PartiqlAst.Expr.Gte): PartiqlAst.Expr {
            val nAry = super.transformExprGte(node) as PartiqlAst.Expr.Gte
            return if (operandsAreComparable(nAry.operands, ">=", nAry.metas)) {
                transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, it, ::getTypeForNAryCompareOperations) }
            } else {
                return nAry.withStaticType(StaticType.BOOL)
            }
        }

        override fun transformExprLt(node: PartiqlAst.Expr.Lt): PartiqlAst.Expr {
            val nAry = super.transformExprLt(node) as PartiqlAst.Expr.Lt
            return if (operandsAreComparable(nAry.operands, "<", nAry.metas)) {
                transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, it, ::getTypeForNAryCompareOperations) }
            } else {
                return nAry.withStaticType(StaticType.BOOL)
            }
        }

        override fun transformExprLte(node: PartiqlAst.Expr.Lte): PartiqlAst.Expr {
            val nAry = super.transformExprLte(node) as PartiqlAst.Expr.Lte
            return if (operandsAreComparable(nAry.operands, "<=", nAry.metas)) {
                transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, it, ::getTypeForNAryCompareOperations) }
            } else {
                return nAry.withStaticType(StaticType.BOOL)
            }
        }

        override fun transformExprBetween(node: PartiqlAst.Expr.Between): PartiqlAst.Expr {
            val nAry = super.transformExprBetween(node) as PartiqlAst.Expr.Between
            val args = listOf(nAry.value, nAry.from, nAry.to)
            return if (operandsAreComparable(args, "BETWEEN", nAry.metas)) {
                transformNAry(nAry, args) { recurseForNAryOperations(nAry, it, ::getTypeForNAryCompareOperations) }
            } else {
                return nAry.withStaticType(StaticType.BOOL)
            }
        }

        // Logical NAry ops: NOT, AND, OR
        override fun transformExprNot(node: PartiqlAst.Expr.Not): PartiqlAst.Expr {
            val nAry = super.transformExprNot(node) as PartiqlAst.Expr.Not
            val args = listOf(nAry.expr)
            return if (hasValidOperandTypes(args, { it is BoolType }, "NOT", nAry.metas)) {
            transformNAry(nAry, args) { recurseForNAryOperations(nAry, it, ::getTypeForNAryLogicalOperations) }
        } else {
                nAry.withStaticType(StaticType.BOOL)
            }
        }

        override fun transformExprAnd(node: PartiqlAst.Expr.And): PartiqlAst.Expr {
            val nAry = super.transformExprAnd(node) as PartiqlAst.Expr.And
            return if (hasValidOperandTypes(nAry.operands, { it is BoolType }, "AND", nAry.metas)) {
            transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, it, ::getTypeForNAryLogicalOperations) }
        } else {
                nAry.withStaticType(StaticType.BOOL)
            }
        }

        override fun transformExprOr(node: PartiqlAst.Expr.Or): PartiqlAst.Expr {
            val nAry = super.transformExprOr(node) as PartiqlAst.Expr.Or
            return if (hasValidOperandTypes(nAry.operands, { it is BoolType }, "OR", nAry.metas)) {
            transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, it, ::getTypeForNAryLogicalOperations) }
        } else {
                nAry.withStaticType(StaticType.BOOL)
            }
        }

        // IN NAry op
        override fun transformExprInCollection(node: PartiqlAst.Expr.InCollection): PartiqlAst.Expr {
            val nAry = super.transformExprInCollection(node) as PartiqlAst.Expr.InCollection
            val operands = nAry.operands.map { it.getStaticType() }
            val lhs = operands[0]
            val rhs = operands[1]
            var errorAdded = false

            // check if any operands are unknown, then null or missing error
            if (operands.any { operand -> operand.isUnknown() }) {
                handleExpressionAlwaysReturnsNullOrMissingError(nAry.metas.getSourceLocation())
                errorAdded = true
            }

            // if none of the [rhs] types are [CollectionType]s with comparable element types to [lhs], then data type
            // mismatch error
            if (!rhs.isUnknown() && rhs.allTypes.none { it is CollectionType && it.elementType.isComparableTo(lhs) }) {
                handleIncompatibleDataTypesForOpError(operands, "IN", nAry.metas.getSourceLocation())
                errorAdded = true
            }
            return if (errorAdded) {
                nAry.withStaticType(StaticType.BOOL)
            } else {
                transformNAry(nAry, nAry.operands) { computeReturnTypeForNAryIn(it) }
            }
        }

        // CONCAT NAry op
        override fun transformExprConcat(node: PartiqlAst.Expr.Concat): PartiqlAst.Expr {
            val nAry = super.transformExprConcat(node) as PartiqlAst.Expr.Concat
            val operandsTypes = nAry.operands.map { it.getStaticType() }

            // check if any non-unknown operand has no text type. if so, then data type mismatch
            return if (hasValidOperandTypes(nAry.operands, { it.isText() }, "||", nAry.metas)) {
            transformNAry(nAry, nAry.operands) { recurseForNAryOperations(nAry, operandsTypes, ::getTypeForNAryStringConcat) }
        } else {
                nAry.withStaticType(StaticType.STRING)
            }
        }

        // LIKE NAry op
        override fun transformExprLike(node: PartiqlAst.Expr.Like): PartiqlAst.Expr {
            val nAry = super.transformExprLike(node) as PartiqlAst.Expr.Like
            val args = listOfNotNull(nAry.value, nAry.pattern, nAry.escape)

            // check if any non-unknown operand has no text type. if so, then data type mismatch
            return if (hasValidOperandTypes(args, { it.isText() }, "LIKE", nAry.metas)) {
            transformNAry(nAry, args) { recurseForNAryOperations(nAry, it, ::getTypeForNAryLike) }
        } else {
                nAry.withStaticType(StaticType.BOOL)
            }
        }

        // CALL
        override fun transformExprCall(node: PartiqlAst.Expr.Call): PartiqlAst.Expr {
            val nAry = super.transformExprCall(node) as PartiqlAst.Expr.Call

            val funcExpr = nAry.funcName
            val functionArguments = nAry.args

            val functionName = funcExpr.text

            val signature = allFunctions[functionName]
            if (signature == null) {
                handleNoSuchFunctionError(functionName, nAry.metas.getSourceLocation())
                return nAry.withStaticType(StaticType.ANY)
            }

            return nAry.withStaticType(computeReturnTypeForFunctionCall(signature, functionArguments, nAry.metas))
        }

        // Call agg : "count", "avg", "max", "min", "sum"
        override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr {
            val nAry = super.transformExprCallAgg(node) as PartiqlAst.Expr.CallAgg
            val funcName = nAry.funcName
            // unwrap the type if this is a collectionType
            val argType = when (val type = nAry.arg.getStaticType()) {
                is CollectionType -> type.elementType
                else -> type
            }
            val sourceLocation = nAry.getStartingSourceLocationMeta()
            return nAry.withStaticType(computeReturnTypeForAggFunc(funcName, argType, sourceLocation))
        }

        fun handleInvalidInputTypeForAggFun(sourceLocation: SourceLocationMeta, aggFunc: SymbolPrimitive, actualType: StaticType, expectedType: StaticType) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocation,
                    details = SemanticProblemDetails.InvalidArgumentTypeForFunction(
                        functionName = aggFunc.text,
                        expectedType = expectedType,
                        actualType = actualType
                    )
                )
            )
        }

        private fun computeReturnTypeForAggFunc(aggFunc: SymbolPrimitive, argType: StaticType, sourceLocation: SourceLocationMeta): StaticType {
            // nested type is not supported by agg functions.
            // i.e. sum(1,2,[3,4]) is not supported
            val isNullOrMissingOrNumeric: (StaticType) -> Boolean = {
                it.isSubTypeOf(StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.NUMERIC))
            }

            return when (aggFunc.text) {
                // current implementation of count will always return a long
                "count" -> StaticType.INT8
                // max/min supports all type and the result depends on comparison.
                // aggregate function will not return missing as a potential Type
                // in case that argType contains only missing,
                "max", "min" -> {
                    val possibleReturnTypes = argType.allTypes.filter {
                        it !is MissingType
                    }
                    if (possibleReturnTypes.isEmpty())
                        StaticType.NULL
                    else
                        StaticType.unionOf(possibleReturnTypes.toSet()).flatten()
                }
                // current implementation of avg always return a decimal or null.
                "avg" -> {
                    when {
                        !isNullOrMissingOrNumeric(argType) -> {
                            handleInvalidInputTypeForAggFun(sourceLocation, aggFunc, argType, StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.NUMERIC))
                            StaticType.unionOf(StaticType.NULL, StaticType.DECIMAL)
                        }
                        // missing, null, or missing and null
                        argType.isSubTypeOf(StaticType.unionOf(StaticType.MISSING, StaticType.NULL)) -> StaticType.NULL
                        else -> StaticType.DECIMAL
                    }
                }
                "sum" -> {
                    if (isNullOrMissingOrNumeric(argType)) {
                        argType.allTypes.fold((StaticType.NULL as SingleType)) { lastType, currentType ->
                            when (currentType) {
                                is MissingType -> lastType
                                is NullType -> lastType
                                is IntType -> {
                                    // based on the current implementation of arithmetic operations
                                    // decimal type precision to be determined
                                    when (lastType) {
                                        is IntType -> {
                                            when {
                                                lastType.rangeConstraint == IntType.IntRangeConstraint.UNCONSTRAINED -> lastType
                                                currentType.rangeConstraint == IntType.IntRangeConstraint.UNCONSTRAINED -> currentType
                                                lastType.rangeConstraint.numBytes > currentType.rangeConstraint.numBytes -> lastType
                                                else -> currentType
                                            }
                                        }
                                        is FloatType -> StaticType.FLOAT
                                        is DecimalType -> StaticType.DECIMAL // TODO:  account for decimal precision
                                        // only missing and null are possible because of argTypeCheck()
                                        else -> currentType
                                    }
                                }
                                is FloatType -> {
                                    when (lastType) {
                                        is IntType -> StaticType.FLOAT
                                        is FloatType -> StaticType.FLOAT
                                        is DecimalType -> StaticType.DECIMAL // TODO:  account for decimal precision
                                        else -> currentType
                                    }
                                }
                                is DecimalType -> {
                                    when (lastType) {
                                        is IntType -> StaticType.DECIMAL // TODO:  account for decimal precision
                                        is FloatType -> StaticType.DECIMAL // TODO:  account for decimal precision
                                        is DecimalType -> StaticType.DECIMAL // TODO:  account for decimal precision
                                        else -> currentType
                                    }
                                }
                                else ->
                                    // this should not be reached, only exists to segment the logic.
                                    error(
                                        "Internal Error: SUM function only support Number Type. " +
                                            "This probably indicates an bug in type inferencer."
                                    )
                            }
                        }
                    }
                    // continuation in case of data type mismatch
                    else {
                        val expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.NUMERIC)
                        handleInvalidInputTypeForAggFun(sourceLocation, aggFunc, argType, expectedType)
                        expectedType
                    }
                }
                // unsupported agg function. This should be caught by the parser
                else -> error("Internal Error: Unsupported aggregate function. This probably indicates a parser bug.")
            }
        }

        /**
         * Gives [SemanticProblemDetails.IncompatibleDatatypesForOp] error when none of the non-unknown [operands]'
         * types satisfy [operandTypeValidator]. Also gives [SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing]
         * error when one of the operands is an unknown. Returns true if none of these errors are added.
         */
        private fun hasValidOperandTypes(
            operands: List<PartiqlAst.Expr>,
            operandTypeValidator: (StaticType) -> Boolean,
            op: String,
            metas: MetaContainer
        ): Boolean {
            val operandsTypes = operands.map { it.getStaticType() }
            var hasValidOperands = true

            // check for an incompatible operand type
            if (operandsTypes.any { operand -> !operand.isUnknown() && operand.allTypes.none(operandTypeValidator) }) {
                handleIncompatibleDataTypesForOpError(operandsTypes, op, metas.getSourceLocation())
                hasValidOperands = false
            }

            // check for an unknown operand type
            if (operandsTypes.any { operand -> operand.isUnknown() }) {
                handleExpressionAlwaysReturnsNullOrMissingError(metas.getSourceLocation())
                hasValidOperands = false
            }

            return hasValidOperands
        }

        private fun computeReturnTypeForArithmeticUnary(expr: PartiqlAst.Expr, operands: List<PartiqlAst.Expr>, op: String): StaticType {
            require(operands.size == 1) { "Unary operations must have one argument" }

            val argType = operands.single().getStaticType()

            // check if [argType] could be a numeric type
            return if (hasValidOperandTypes(operands, { it.isNumeric() }, op, expr.metas)) {
            val allTypes = argType.allTypes
            val possibleReturnTypes = allTypes.map { st ->
                when (st) {
                    is IntType, is FloatType, is DecimalType -> st
                    is NullType -> StaticType.NULL
                    else -> StaticType.MISSING
                }
            }.distinct()

            when (possibleReturnTypes.size) {
                1 -> possibleReturnTypes.single()
                else -> StaticType.unionOf(*possibleReturnTypes.toTypedArray())
            }
        } else {
                // continuation type of all numeric types to prevent incompatible types and unknown errors from propagating
                StaticType.unionOf(StaticType.ALL_TYPES.filter { it.isNumeric() }.toSet())
            }
        }

        private fun computeReturnTypeForArithmeticNAry(expr: PartiqlAst.Expr, operands: List<PartiqlAst.Expr>, op: String): StaticType {
            // check if all operands could be a numeric type
            return if (hasValidOperandTypes(operands, { it.isNumeric() }, op, expr.metas)) {
            operands.map { it.getStaticType() }.reduce { lastType, currentType ->
                when {
                    lastType is MissingType || currentType is MissingType -> StaticType.MISSING
                    lastType is NullType || currentType is NullType -> StaticType.NULL
                    else -> {
                        val leftTypes = lastType.allTypes
                        val rightTypes = currentType.allTypes

                        val possibleResultTypes: List<SingleType> =
                            leftTypes.flatMap { type1 ->
                                rightTypes.map { type2 ->
                                    computeBinaryArithmeticResultType(type1, type2)
                                }
                            }.distinct()

                        when (possibleResultTypes.size) {
                            0 -> error("We always expect there to be at least one possible result type, even if is MISSING")
                            1 -> {
                                // returning StaticType.MISSING from this branch is an error condition because the
                                // arithmetic operation can *never* succeed.
                                possibleResultTypes.first()
                            }
                            else -> AnyOfType(possibleResultTypes.toSet())
                        }
                    }
                }
            }
        } else {
                // continuation type of all numeric types to prevent incompatible types and unknown errors from propagating
                StaticType.unionOf(StaticType.ALL_TYPES.filter { it.isNumeric() }.toSet())
            }
        }

        private fun computeBinaryArithmeticResultType(leftType: StaticType, rightType: StaticType): SingleType =
            // This could also have been a lookup table of types, however... doing this as a nested `when` allows
            // us to not to rely on `.equals` and `.hashcode` implementations of [StaticType], which include metas
            // and might introduce unwanted behavior.
            when {
                // Propagate missing as missing. Missing has precedence over null
                leftType is MissingType || rightType is MissingType -> StaticType.MISSING
                leftType is NullType || rightType is NullType -> StaticType.NULL
                else -> when (leftType) {
                    is IntType ->
                        when (rightType) {
                            is IntType ->
                                when {
                                    leftType.rangeConstraint == IntType.IntRangeConstraint.UNCONSTRAINED -> leftType
                                    rightType.rangeConstraint == IntType.IntRangeConstraint.UNCONSTRAINED -> rightType
                                    leftType.rangeConstraint.numBytes > rightType.rangeConstraint.numBytes -> leftType
                                    else -> rightType
                                }
                            is FloatType -> StaticType.FLOAT
                            is DecimalType -> StaticType.DECIMAL // TODO:  account for decimal precision
                            else -> StaticType.MISSING
                        }
                    is FloatType ->
                        when (rightType) {
                            is IntType -> StaticType.FLOAT
                            is FloatType -> StaticType.FLOAT
                            is DecimalType -> StaticType.DECIMAL // TODO:  account for decimal precision
                            else -> StaticType.MISSING
                        }
                    is DecimalType ->
                        when (rightType) {
                            is IntType -> StaticType.DECIMAL // TODO:  account for decimal precision
                            is FloatType -> StaticType.DECIMAL // TODO:  account for decimal precision
                            is DecimalType -> StaticType.DECIMAL // TODO:  account for decimal precision
                            else -> StaticType.MISSING
                        }
                    else -> StaticType.MISSING
                }
            }

        private fun computeReturnTypeForNAryIn(argTypes: List<StaticType>): StaticType {
            require(argTypes.size >= 2) { "IN must have at least two args" }
            val leftTypes = argTypes.first().allTypes
            val rightTypes = argTypes.drop(1).flatMap { it.allTypes }

            val finalTypes = leftTypes
                .flatMap { left ->
                    rightTypes.flatMap { right ->
                        computeReturnTypeForBinaryIn(left, right).allTypes
                    }
                }.distinct()

            return when (finalTypes.size) {
                1 -> finalTypes.first()
                else -> StaticType.unionOf(*finalTypes.toTypedArray())
            }
        }

        private fun computeReturnTypeForBinaryIn(left: StaticType, right: StaticType): StaticType =
            when (right) {
                is NullType -> when (left) {
                    is MissingType -> StaticType.MISSING
                    else -> StaticType.NULL
                }
                is MissingType -> StaticType.MISSING
                is CollectionType -> when (left) {
                    is NullType -> StaticType.NULL
                    is MissingType -> StaticType.MISSING
                    else -> {
                        val rightElemTypes = right.elementType.allTypes
                        val possibleTypes = mutableSetOf<StaticType>()
                        if (rightElemTypes.any { it is MissingType }) {
                            possibleTypes.add(StaticType.MISSING)
                        }
                        if (rightElemTypes.any { it is NullType }) {
                            possibleTypes.add(StaticType.NULL)
                        }
                        if (rightElemTypes.any { !it.isNullOrMissing() }) {
                            possibleTypes.add(StaticType.BOOL)
                        }
                        StaticType.unionOf(possibleTypes).flatten()
                    }
                }
                else -> when (left) {
                    is NullType -> StaticType.unionOf(StaticType.NULL, StaticType.MISSING)
                    else -> StaticType.MISSING
                }
            }

        /**
         * Helper to recurse for NAry operations when one or more arguments contain union types.
         *
         * Will "open up" union types and generates the possible combination of types within them
         * and calls [opTypeFunc] for each arg type combination.
         */
        fun recurseForNAryOperations(
            nAryOp: PartiqlAst.Expr,
            args: List<StaticType>,
            opTypeFunc: (PartiqlAst.Expr, List<SingleType>) -> StaticType
        ): StaticType {
            val argsAllTypes = args.map { it.allTypes }
            val cartesianProductOfArgTypes = argsAllTypes.cartesianProduct()

            val possibleResultTypes = cartesianProductOfArgTypes.map { argTypes ->
                val singleTypeArgs = argTypes.map {
                    if (it is SingleType) {
                        it
                    } else {
                        error("Expected only SingleType to be present")
                    }
                }
                opTypeFunc(nAryOp, singleTypeArgs)
            }.toSet()
            return AnyOfType(possibleResultTypes).flatten()
        }

        /**
         * Infers type for NAry comparison operations when all the arguments are of type [SingleType].
         */
        fun getTypeForNAryCompareOperations(nAryOp: PartiqlAst.Expr, args: List<SingleType>): SingleType {
            if (args.size < 2) {
                error("Expected 2 or more operands for $nAryOp")
            }
            if (args.size < 3 && nAryOp is PartiqlAst.Expr.Between) {
                error("Expected 3 or more operands for $nAryOp")
            }

            return when {
                // If any of the operands is MISSING, return MISSING. MISSING has a precedence over NULL
                args.any { it is MissingType } -> StaticType.MISSING
                // If any of the operands is NULL, return NULL
                args.any { it is NullType } -> StaticType.NULL
                // Comparison between different types of arguments (except null/missing)
                // always returns a boolean for equality and inequality operator
                nAryOp is PartiqlAst.Expr.Eq || nAryOp is PartiqlAst.Expr.Ne -> StaticType.BOOL
                // Comparison between different kinds of number types is allowed
                args.all { it.isNumeric() } -> StaticType.BOOL
                // Comparison between string and symbols is allowed
                args.all { it.isText() } -> StaticType.BOOL
                // Comparison between blobs and clobs is allowed
                args.all { it.isLob() } -> StaticType.BOOL
                // They all have the same runtimeType type. Comparison is allowed
                args.map { it.runtimeType }.distinct().count() == 1 -> StaticType.BOOL
                else -> StaticType.MISSING
            }
        }

        /**
         * Infers type for NAry logical operations when all the arguments are of type [SingleType]
         */
        fun getTypeForNAryLogicalOperations(nAryOp: PartiqlAst.Expr, args: List<SingleType>): StaticType {
            if ((args.size != 1) && (nAryOp is PartiqlAst.Expr.Not)) {
                error("Expected 1 operand for $nAryOp")
            }
            if ((args.size < 2) && (nAryOp !is PartiqlAst.Expr.Not)) {
                error("Expected 2 or more operands for $nAryOp")
            }

            return when {
                // Logical operands need to be of Boolean Type
                args.all { it == StaticType.BOOL } -> StaticType.BOOL
                // If any of the arguments is boolean, then the return type can be boolean because of short-circuiting
                // in logical ops. For e.g. "TRUE OR ANY" returns TRUE. "FALSE AND ANY" returns FALSE. But in the case
                // where the other arg is an incompatible type (not an unknown or bool), the result type is MISSING.
                args.any { it == StaticType.BOOL } -> when {
                    // If other argument is missing, then return union(bool, missing)
                    args.any { it is MissingType } -> AnyOfType(setOf(StaticType.MISSING, StaticType.BOOL))
                    // If other argument is null, then return union(bool, null)
                    args.any { it is NullType } -> AnyOfType(setOf(StaticType.NULL, StaticType.BOOL))
                    // If other type is anything other than null or missing, then it is an error case
                    else -> StaticType.MISSING
                }
                // If any of the operands is MISSING, return MISSING. MISSING has a precedence over NULL
                args.any { it is MissingType } -> StaticType.MISSING
                // If any of the operands is NULL, return NULL
                args.any { it is NullType } -> StaticType.NULL
                else -> StaticType.MISSING
            }
        }

        /**
         * Infers type for the IN operation when all the arguments are of type [SingleType]
         */
        fun getTypeForNAryIn(nAryOp: PartiqlAst.Expr, args: List<SingleType>): SingleType {
            if (args.size < 2) {
                error("Expected 2 or more operands for $nAryOp")
            }

            return when {
                // If any of the operands is MISSING, return MISSING. MISSING has a precedence over NULL
                args.any { it is MissingType } -> StaticType.MISSING
                // If any of the operands is NULL, return NULL
                args.any { it is NullType } -> StaticType.NULL
                // Right arg should be collection type for IN operator.
                args[1] is CollectionType -> StaticType.BOOL
                else -> StaticType.MISSING
            }
        }

        /**
         * Computes the constraints for the string concatenation if all the arguments are [StringType].
         *
         */
        fun computeConstraintsForConcatStringType(args: List<StringType>): SingleType {
            if (args.size < 2) {
                error("Expected 2 or more operands for CONCAT")
            }
            val constraints = args.map { it.lengthConstraint }
            val lengths: List<StringType.StringLengthConstraint.Constrained> = constraints.map {
                when (it) {
                    // Return Unconstrained string when one of the StringTypes is Unconstrained
                    is StringType.StringLengthConstraint.Unconstrained -> return StaticType.STRING
                    is StringType.StringLengthConstraint.Constrained -> it
                }
            }
            val maximumLength = lengths.sumBy { it.length.value }
            val newNumberConstraint = when {
                lengths.all { it.length is NumberConstraint.Equals } -> NumberConstraint.Equals(maximumLength)
                else -> NumberConstraint.UpTo(maximumLength)
            }
            return StringType(StringType.StringLengthConstraint.Constrained(newNumberConstraint))
        }

        /**
         * Infers type for the concat operation when all the arguments are of type [SingleType]
         */
        fun getTypeForNAryStringConcat(nAryOp: PartiqlAst.Expr, args: List<SingleType>): SingleType {
            if (args.size < 2) {
                error("Expected 2 or more operands for $nAryOp")
            }

            val stringArgTypes = args.filterIsInstance<StringType>()
            return when {
                // If any one of the operands is missing, return MISSING. MISSING has precedence over NULL
                args.any { it is MissingType } -> StaticType.MISSING
                // If any one of the operands is null, return NULL
                args.any { it is NullType } -> StaticType.NULL
                // If all the types are StringTypes, then add the string constraints accordingly
                stringArgTypes.size == args.size -> computeConstraintsForConcatStringType(stringArgTypes)
                // Arguments for string_concat need to be text type
                args.all { it.isText() } -> StaticType.STRING
                else -> StaticType.MISSING
            }
        }

        /**
         * Infers type for the LIKE operation when all the arguments are of type [SingleType]
         *
         * If the optional escape character is provided, it can result in failure even if the type is text (string, in this case)
         * This is because the escape character needs to be a single character (string with length 1),
         * Even if the escape character is of length 1, escape sequence can be incorrect.
         * Check [EvaluatingCompiler.checkPattern] method for more details.
         */
        fun getTypeForNAryLike(
            @Suppress("UNUSED_PARAMETER")
            nAryOp: PartiqlAst.Expr,
            args: List<SingleType>
        ): StaticType {
            return when {
                // If any one of the operands is missing, return MISSING. MISSING has precedence over NULL
                args.any { it is MissingType } -> StaticType.MISSING
                // If any one of the operands is null, return NULL
                args.any { it is NullType } -> StaticType.NULL
                // Arguments for LIKE need to be text type
                args.all { it.isText() } -> when (args.size) {
                    2 -> StaticType.BOOL
                    // if optional escape character is provided, it may result in error or boolean.
                    else -> AnyOfType(setOf(StaticType.MISSING, StaticType.BOOL))
                }
                else -> StaticType.MISSING
            }
        }

        /**
         * Computes the return type of the function call based on the [FunctionSignature.unknownArguments]
         */
        private fun computeReturnTypeForFunctionCall(signature: FunctionSignature, arguments: List<PartiqlAst.Expr>, functionMetas: MetaContainer): StaticType {
            // Check for all the possible invalid number of argument cases. Throws an error if invalid number of arguments found.
            if (!signature.arity.contains(arguments.size)) {
                handleIncorrectNumberOfArgumentsToFunctionCallError(signature.name, signature.arity, arguments.size, functionMetas.getSourceLocation())
            }

            return when (signature.unknownArguments) {
                UnknownArguments.PROPAGATE -> returnTypeForPropagatingFunction(signature, arguments)
                UnknownArguments.PASS_THRU -> returnTypeForPassThruFunction(signature, arguments)
            }
        }

        /**
         * Computes return type for functions with [FunctionSignature.unknownArguments] as [UnknownArguments.PASS_THRU]
         */
        private fun returnTypeForPassThruFunction(signature: FunctionSignature, arguments: List<PartiqlAst.Expr>): StaticType {
            return when {
                matchesAllArguments(arguments, signature) -> signature.returnType
                matchesAtLeastOneArgument(arguments, signature) -> StaticType.unionOf(signature.returnType, StaticType.MISSING)
                else -> StaticType.MISSING
            }
        }

        /**
         * Returns true if for every pair (expr, expectedType) in [argsWithExpectedTypes], the expr's [StaticType] is
         * not an unknown and has a shared type with expectedType. Returns false otherwise.
         *
         * If an argument has an unknown type, the [SemanticProblemDetails.NullOrMissingFunctionArgument] error is
         * handled by [problemHandler]. If an expr has no shared type with the expectedType, the
         * [SemanticProblemDetails.InvalidArgumentTypeForFunction] error is handled by [problemHandler].
         */
        private fun functionHasValidArgTypes(functionName: String, argsWithExpectedTypes: List<Pair<PartiqlAst.Expr, StaticType>>): Boolean {
            var allArgsValid = true
            argsWithExpectedTypes.forEach { (actualExpr, expectedType) ->
                val actualType = actualExpr.getStaticType()

                if (actualType.isUnknown()) {
                    handleNullOrMissingFunctionArgument(functionName, actualExpr.metas.getSourceLocation())
                    allArgsValid = false
                } else {
                    val actualNonUnknownType = actualType.filterNullMissing()
                    if (actualNonUnknownType.typeDomain.intersect(expectedType.typeDomain).isEmpty()) {
                        handleInvalidArgumentTypeForFunction(
                            functionName = functionName,
                            expectedType = expectedType,
                            actualType = actualType,
                            sourceLocationMeta = actualExpr.metas.getSourceLocation()
                        )
                        allArgsValid = false
                    }
                }
            }
            return allArgsValid
        }

        /**
         * Computes return type for functions with [FunctionSignature.unknownArguments] as [UnknownArguments.PROPAGATE]
         */
        private fun returnTypeForPropagatingFunction(signature: FunctionSignature, arguments: List<PartiqlAst.Expr>): StaticType {
            val requiredArgs = arguments.zip(signature.requiredParameters)
            val restOfArgs = arguments.drop(signature.requiredParameters.size)

            // all args including optional/variadic
            val allArgs = requiredArgs.let { reqArgs ->
                if (restOfArgs.isNotEmpty()) {
                    when {
                        signature.optionalParameter != null -> reqArgs + listOf(Pair(restOfArgs.first(), signature.optionalParameter))
                        signature.variadicParameter != null -> reqArgs + restOfArgs.map {
                            Pair(it, signature.variadicParameter.type)
                        }
                        else -> reqArgs
                    }
                } else {
                    reqArgs
                }
            }

            return if (functionHasValidArgTypes(signature.name, allArgs)) {
                val finalReturnTypes = signature.returnType.allTypes + allArgs.flatMap { (actualExpr, expectedType) ->
                    val actualType = actualExpr.getStaticType()
                    listOfNotNull(
                        // if any type is `MISSING`, add `MISSING` to possible return types.
                        // if the actual type is not a subtype is the expected type, add `MISSING`. In the future, may
                        // want to give a warning that a data type mismatch could occur
                        // (https://github.com/partiql/partiql-lang-kotlin/issues/507)
                        StaticType.MISSING.takeIf {
                            actualType.allTypes.any { it is MissingType } || !actualType.filterNullMissing().isSubTypeOf(expectedType)
                        },
                        // if any type is `NULL`, add `NULL` to possible return types
                        StaticType.NULL.takeIf { actualType.allTypes.any { it is NullType } }
                    )
                }
                AnyOfType(finalReturnTypes.toSet()).flatten()
            } else {
                // otherwise, has an invalid arg type and errors. continuation type of [FunctionSignature.returnType]
                signature.returnType
            }
        }

        /**
         * Function assumes the number of [arguments] passed agrees with the [signature]
         *
         * Returns true if there's at least one valid overlap between actual and expected
         * for all the expected arguments (required, optional, variadic) for the [signature].
         *
         * Returns false otherwise.
         */
        private fun matchesAtLeastOneArgument(arguments: List<PartiqlAst.Expr>, signature: FunctionSignature): Boolean {
            val requiredArgumentsMatch = arguments
                .zip(signature.requiredParameters)
                .all { (actual, expected) ->
                    actual.getStaticType().typeDomain.intersect(expected.typeDomain).isNotEmpty()
                }

            val optionalArgumentMatches = when (signature.optionalParameter) {
                null -> true
                else ->
                    arguments
                        .getOrNull(signature.requiredParameters.size)
                        ?.getStaticType()?.typeDomain
                        ?.intersect(signature.optionalParameter.typeDomain)
                        ?.isNotEmpty()
                        ?: true
            }

            val variadicArgumentsMatch = when (signature.variadicParameter) {
                null -> true
                else ->
                    arguments
                        .drop(signature.requiredParameters.size)
                        .all { arg ->
                            val argType = arg.getStaticType()
                            argType.typeDomain.intersect(signature.variadicParameter.type.typeDomain).isNotEmpty()
                        }
            }

            return requiredArgumentsMatch && optionalArgumentMatches && variadicArgumentsMatch
        }

        /**
         * Function assumes the number of [arguments] passed agrees with the [signature]
         * Returns true when all the arguments (required, optional, variadic) are subtypes of the expected arguments for the [signature].
         * Returns false otherwise
         */
        private fun matchesAllArguments(arguments: List<PartiqlAst.Expr>, signature: FunctionSignature): Boolean {
            // Checks if the actual StaticType is subtype of expected StaticType ( filtering the null/missing for PROPAGATING functions
            fun isSubType(actual: StaticType, expected: StaticType) =
                when (signature.unknownArguments) {
                    UnknownArguments.PROPAGATE -> when (actual) {
                        is AnyOfType -> actual.copy(
                            types = actual.types.filter {
                                !it.isNullOrMissing()
                            }.toSet()
                        )
                        else -> actual
                    }
                    UnknownArguments.PASS_THRU -> actual
                }
                    .isSubTypeOf(expected)

            val requiredArgumentsMatch = arguments
                .zip(signature.requiredParameters)
                .all { (actual, expected) ->
                    val st = actual.getStaticType()
                    isSubType(st, expected)
                }

            val optionalArgumentMatches = when (signature.optionalParameter) {
                null -> true
                else -> {
                    val st = arguments
                        .getOrNull(signature.requiredParameters.size)
                        ?.getStaticType()
                    when (st) {
                        null -> true
                        else -> isSubType(st, signature.optionalParameter)
                    }
                }
            }

            val variadicArgumentsMatch = when (signature.variadicParameter) {
                null -> true
                else ->
                    arguments
                        // We make an assumption here that either the optional or the variadic arguments are passed to the function.
                        // This "drop" may not hold true if both, optional and variadic arguments, are allowed at the same time.
                        .drop(signature.requiredParameters.size)
                        .all { arg ->
                            val st = arg.getStaticType()
                            isSubType(st, signature.variadicParameter.type)
                        }
            }

            return requiredArgumentsMatch && optionalArgumentMatches && variadicArgumentsMatch
        }

        override fun transformExprLit(node: PartiqlAst.Expr.Lit): PartiqlAst.Expr {
            val literal = super.transformExprLit(node) as PartiqlAst.Expr.Lit
            val exprValueType = ExprValueType.fromIonType(literal.value.type.toIonType())
            return literal.withStaticType(StaticType.fromExprValueType(exprValueType))
        }

        override fun transformExprMissing(node: PartiqlAst.Expr.Missing): PartiqlAst.Expr {
            val literal = super.transformExprMissing(node)
            return literal.withStaticType(StaticType.MISSING)
        }

        // Seq => List, Sexp, Bag
        private fun transformSeq(
            expr: PartiqlAst.Expr,
            values: List<PartiqlAst.Expr>,
            compute: (StaticType) -> StaticType
        ): PartiqlAst.Expr {
            val valuesTypes = AnyOfType(values.map { it.getStaticType() }.toSet()).flatten()
            val inferredType = compute(valuesTypes)
            return expr.withStaticType(inferredType)
        }

        override fun transformExprList(node: PartiqlAst.Expr.List): PartiqlAst.Expr {
            val seq = super.transformExprList(node) as PartiqlAst.Expr.List
            return transformSeq(seq, seq.values) { ListType(it) }
        }

        override fun transformExprSexp(node: PartiqlAst.Expr.Sexp): PartiqlAst.Expr {
            val seq = super.transformExprSexp(node) as PartiqlAst.Expr.Sexp
            return transformSeq(seq, seq.values) { SexpType(it) }
        }

        override fun transformExprBag(node: PartiqlAst.Expr.Bag): PartiqlAst.Expr {
            val seq = super.transformExprBag(node) as PartiqlAst.Expr.Bag
            return transformSeq(seq, seq.values) { BagType(it) }
        }

        override fun transformExprBagOp(node: PartiqlAst.Expr.BagOp): PartiqlAst.Expr {
            val bagOp = super.transformExprBagOp(node) as PartiqlAst.Expr.BagOp
            // TODO assert operand compatibility once SQL bag operators are implemented
            return bagOp.withStaticType(StaticType.BAG)
        }

        override fun transformExprSimpleCase(node: PartiqlAst.Expr.SimpleCase): PartiqlAst.Expr {
            val simpleCase = super.transformExprSimpleCase(node) as PartiqlAst.Expr.SimpleCase
            val caseValue = simpleCase.expr
            val caseValueType = caseValue.getStaticType()

            // comparison never succeeds if caseValue is an unknown
            if (caseValueType.isUnknown()) {
                handleExpressionAlwaysReturnsNullOrMissingError(caseValue.getStartingSourceLocationMeta())
            }

            val whenExprs = simpleCase.cases.pairs.map { expr -> expr.first }
            whenExprs.forEach { whenExpr ->
                val whenExprType = whenExpr.getStaticType()
                // comparison never succeeds if whenExpr is unknown -> null or missing error
                if (whenExprType.isUnknown()) {
                    handleExpressionAlwaysReturnsNullOrMissingError(whenExpr.getStartingSourceLocationMeta())
                }

                // if caseValueType is incomparable to whenExprType -> data type mismatch
                else if (!caseValueType.isComparableTo(whenExprType)) {
                    handleIncompatibleDataTypesForOpError(
                        actualTypes = listOf(caseValueType, whenExprType),
                        op = "CASE",
                        sourceLocationMeta = whenExpr.getStartingSourceLocationMeta()
                    )
                }
            }

            val thenExprs = simpleCase.cases.pairs.map { expr -> expr.second }

            // keep all the `THEN` expr types even if the comparison doesn't succeed
            val simpleCaseType = inferCaseWhenBranches(thenExprs, simpleCase.default)
            return simpleCase.withStaticType(simpleCaseType)
        }

        override fun transformExprSearchedCase(node: PartiqlAst.Expr.SearchedCase): PartiqlAst.Expr {
            val searchedCase = super.transformExprSearchedCase(node) as PartiqlAst.Expr.SearchedCase

            val whenExprs = searchedCase.cases.pairs.map { expr -> expr.first }
            whenExprs.forEach { whenExpr ->
                val whenExprType = whenExpr.getStaticType()

                // if whenExpr is unknown -> null or missing error
                if (whenExprType.isUnknown()) {
                    handleExpressionAlwaysReturnsNullOrMissingError(whenExpr.getStartingSourceLocationMeta())
                }

                // if whenExpr can never be bool -> data type mismatch
                else if (whenExprType.allTypes.none { it is BoolType }) {
                    handleIncompatibleDataTypeForExprError(
                        expectedType = StaticType.BOOL,
                        actualType = whenExprType,
                        sourceLocationMeta = whenExpr.getStartingSourceLocationMeta()
                    )
                }
            }

            val thenExprs = searchedCase.cases.pairs.map { expr -> expr.second }

            // keep all the `THEN` expr types even if the whenExpr could never be bool
            val searchedCaseType = inferCaseWhenBranches(thenExprs, searchedCase.default)
            return searchedCase.withStaticType(searchedCaseType)
        }

        fun inferCaseWhenBranches(thenExprs: List<PartiqlAst.Expr>, elseExpr: PartiqlAst.Expr?): StaticType {
            val thenExprsTypes = thenExprs.map { it.getStaticType() }
            val elseExprType = when (elseExpr) {
                // If there is no ELSE clause in the expression, it possible that
                // none of the WHEN clauses succeed and the output of CASE WHEN expression
                // ends up being NULL
                null -> StaticType.NULL
                else -> elseExpr.getStaticType()
            }

            if (thenExprsTypes.any { it is AnyType } || elseExprType is AnyType) {
                return StaticType.ANY
            }

            val possibleTypes = thenExprsTypes + elseExprType
            return AnyOfType(possibleTypes.toSet()).flatten()
        }

        // PIG ast Types => CanCast, CanLosslessCast, IsType, ExprCast
        override fun transformExprCanCast(node: PartiqlAst.Expr.CanCast): PartiqlAst.Expr {
            val typed = super.transformExprCanCast(node) as PartiqlAst.Expr.CanCast
            return typed.withStaticType(StaticType.BOOL)
        }

        override fun transformExprCanLosslessCast(node: PartiqlAst.Expr.CanLosslessCast): PartiqlAst.Expr {
            val typed = super.transformExprCanLosslessCast(node) as PartiqlAst.Expr.CanLosslessCast
            return typed.withStaticType(StaticType.BOOL)
        }

        override fun transformExprIsType(node: PartiqlAst.Expr.IsType): PartiqlAst.Expr {
            val typed = super.transformExprIsType(node) as PartiqlAst.Expr.IsType
            return typed.withStaticType(StaticType.BOOL)
        }

        override fun transformExprCast(node: PartiqlAst.Expr.Cast): PartiqlAst.Expr {
            val typed = super.transformExprCast(node) as PartiqlAst.Expr.Cast
            val sourceType = typed.value.getStaticType()
            val targetType = typed.asType.toTypedOpParameter(customTypedOpParameters)
            val castOutputType = sourceType.cast(targetType.staticType).let {
                if (targetType.validationThunk == null) {
                    // There is no additional validation for this parameter, return this type as-is
                    it
                } else {
                    StaticType.unionOf(StaticType.MISSING, it)
                }
            }
            return typed.withStaticType(castOutputType)
        }

        override fun transformExprNullIf(node: PartiqlAst.Expr.NullIf): PartiqlAst.Expr {
            val nullIf = super.transformExprNullIf(node) as PartiqlAst.Expr.NullIf

            // check for comparability of the two arguments to `NULLIF`
            operandsAreComparable(listOf(nullIf.expr1, nullIf.expr2), "NULLIF", nullIf.metas)

            // output type will be the first argument's types along with `NULL` (even in the case of an error)
            val possibleOutputTypes = nullIf.expr1.getStaticType().asNullable()
            return nullIf.withStaticType(possibleOutputTypes)
        }

        override fun transformExprCoalesce(node: PartiqlAst.Expr.Coalesce): PartiqlAst.Expr {
            val coalesce = super.transformExprCoalesce(node) as PartiqlAst.Expr.Coalesce
            var allMissing = true
            val outputTypes = mutableSetOf<StaticType>()

            for (arg in coalesce.args) {
                val staticTypes = arg.getStaticType().allTypes
                outputTypes += staticTypes
                // If at least one known type is found, remove null and missing from the result
                // It means there is at least one type which doesn't contain unknown types.
                if (staticTypes.all { type -> !type.isNullOrMissing() }) {
                    outputTypes.remove(StaticType.MISSING)
                    outputTypes.remove(StaticType.NULL)
                    break
                }
                if (!staticTypes.contains(StaticType.MISSING)) {
                    allMissing = false
                }
            }
            // If every argument has MISSING as one of it's types,
            // then output should contain MISSING and not otherwise.
            if (!allMissing) {
                outputTypes.remove(StaticType.MISSING)
            }

            return coalesce.withStaticType(
                when (outputTypes.size) {
                    1 -> outputTypes.first()
                    else -> StaticType.unionOf(outputTypes)
                }
            )
        }

        override fun transformExprStruct(node: PartiqlAst.Expr.Struct): PartiqlAst.Expr {
            val struct = super.transformExprStruct(node) as PartiqlAst.Expr.Struct
            val structFields = mutableListOf<Pair<String, StaticType>>()
            var closedContent = true
            struct.fields.forEach { expr ->
                val nameExpr = expr.first
                val valueExpr = expr.second
                when (nameExpr) {
                    is PartiqlAst.Expr.Lit ->
                        // A field is only included in the StructType if its key is a text literal
                        if (nameExpr.value is TextElement) {
                            structFields.add(nameExpr.value.textValue to valueExpr.getStaticType())
                        }
                    else -> {
                        // A field with a non-literal key name is not included.
                        // If the non-literal could be text, StructType will have open content.
                        if (nameExpr.getStaticType().allTypes.any { it.isText() }) {
                            closedContent = false
                        }
                    }
                }
            }

            val hasDuplicateKeys = structFields
                .groupingBy { it.first }
                .eachCount()
                .any { it.value > 1 }

            if (hasDuplicateKeys) {
                TODO("Duplicate keys in struct is not yet handled")
            }

            return struct.withStaticType(StructType(structFields.toMap(), contentClosed = closedContent))
        }

        private fun getElementTypeForFromSource(fromSourceType: StaticType): StaticType =
            when (fromSourceType) {
                is BagType -> fromSourceType.elementType
                is ListType -> fromSourceType.elementType
                is AnyType -> StaticType.ANY
                is AnyOfType -> AnyOfType(fromSourceType.types.map { getElementTypeForFromSource(it) }.toSet())
                // All the other types coerce into a bag of themselves (including null/missing/sexp).
                else -> fromSourceType
            }

        override fun transformFromSourceScan(node: PartiqlAst.FromSource.Scan): PartiqlAst.FromSource {
            val from = super.transformFromSourceScan(node) as PartiqlAst.FromSource.Scan

            val asSymbolicName = node.asAlias
                ?: error("fromSourceLet.asName is null.  This wouldn't be the case if FromSourceAliasVisitorTransform was executed first.")

            val fromExprType = from.expr.getStaticType()

            val elementType = getElementTypeForFromSource(fromExprType)

            addLocal(asSymbolicName.text, elementType)

            node.atAlias?.let {
                val hasLists = fromExprType.typeDomain.contains(ExprValueType.LIST)
                val hasOnlyLists = hasLists && (fromExprType.typeDomain.size == 1)
                when {
                    hasOnlyLists -> {
                        addLocal(it.text, StaticType.INT)
                    }
                    hasLists -> {
                        addLocal(it.text, StaticType.unionOf(StaticType.INT, StaticType.MISSING))
                    }
                    else -> {
                        addLocal(it.text, StaticType.MISSING)
                    }
                }
            }

            node.byAlias?.let {
                TODO("BY variable's inference is not implemented yet.")
            }

            return from
        }

        /**
         * Verifies the given [expr]'s [StaticType] has type [expectedType]. If [expr] is unknown, a null or missing
         * error is given. If [expr]'s [StaticType] could never be [expectedType], an incompatible data types for
         * expression error is given.
         */
        private fun verifyExpressionType(expr: PartiqlAst.Expr, expectedType: StaticType) {
            val exprType = expr.getStaticType()

            if (exprType.isUnknown()) {
                handleExpressionAlwaysReturnsNullOrMissingError(expr.getStartingSourceLocationMeta())
            } else if (exprType.allTypes.none { it == expectedType }) {
                handleIncompatibleDataTypeForExprError(
                    expectedType = expectedType,
                    actualType = exprType,
                    sourceLocationMeta = expr.getStartingSourceLocationMeta()
                )
            }
        }

        /**
         * Replaces `null` predicates with literal `true` with type [StaticType.BOOL].
         *
         * In the PartiqlAst, the non-present, optional join predicate is represented with a `null` value, even
         * though the `null` predicate is equivalent to `true`.  However, that also causes it to be skipped and not
         * assigned a `StaticType`, which is required by [EvaluatorStaticTypeTests].
         *
         * If predicate is non-null, checks that its type could be [StaticType.BOOL]. If the type is an unknown, gives
         * a null or missing error. If the type is not unknown and could never be [StaticType.BOOL], gives a data type
         * mismatch error (incompatible types for expression).
         */
        override fun transformFromSourceJoin_predicate(node: PartiqlAst.FromSource.Join): PartiqlAst.Expr? {
            return when (val predicate = super.transformFromSourceJoin_predicate(node)) {
                null -> PartiqlAst.build { lit(ionBool(true)).withStaticType(StaticType.BOOL) }
                else -> {
                    // verify `JOIN` predicate is bool. If it's unknown, gives a null or missing error. If it could
                    // never be a bool, gives an incompatible data type for expression error
                    verifyExpressionType(expr = predicate, expectedType = StaticType.BOOL)

                    // continuation type (even in the case of an error) is [StaticType.BOOL]
                    predicate.withStaticType(StaticType.BOOL)
                }
            }
        }

        private fun getUnpivotValueType(fromSourceType: StaticType): StaticType =
            when (fromSourceType) {
                is StructType -> if (fromSourceType.contentClosed) {
                    AnyOfType(fromSourceType.fields.values.toSet()).flatten()
                } else {
                    // Content is open, so value can be of any type
                    StaticType.ANY
                }
                is AnyType -> StaticType.ANY
                is AnyOfType -> AnyOfType(fromSourceType.types.map { getUnpivotValueType(it) }.toSet())
                // All the other types coerce into a struct of themselves with synthetic key names
                else -> fromSourceType
            }

        override fun transformFromSourceUnpivot(node: PartiqlAst.FromSource.Unpivot): PartiqlAst.FromSource {
            val from = super.transformFromSourceUnpivot(node) as PartiqlAst.FromSource.Unpivot

            val asSymbolicName = node.asAlias
                ?: error("FromSourceUnpivot.asAlias is null.  This wouldn't be the case if FromSourceAliasVisitorTransform was executed first.")

            val fromExprType = from.expr.getStaticType()

            val valueType = getUnpivotValueType(fromExprType)
            addLocal(asSymbolicName.text, valueType)

            node.atAlias?.let {
                val valueHasMissing = valueType.typeDomain.contains(ExprValueType.MISSING)
                val valueOnlyHasMissing = valueHasMissing && valueType.typeDomain.size == 1
                when {
                    valueOnlyHasMissing -> {
                        addLocal(it.text, StaticType.MISSING)
                    }
                    valueHasMissing -> {
                        addLocal(it.text, StaticType.STRING.asOptional())
                    }
                    else -> {
                        addLocal(it.text, StaticType.STRING)
                    }
                }
            }

            node.byAlias?.let {
                TODO("BY variable's inference is not implemented yet.")
            }

            return from
        }

        override fun transformExprPath(node: PartiqlAst.Expr.Path): PartiqlAst.Expr {
            val path = super.transformExprPath(node) as PartiqlAst.Expr.Path
            var currentType = path.root.getStaticType()
            val newComponents = path.steps.map { pathComponent ->
                currentType = when (pathComponent) {
                    is PartiqlAst.PathStep.PathExpr -> inferPathComponentExprType(currentType, pathComponent)
                    is PartiqlAst.PathStep.PathUnpivot -> TODO("PathUnpivot is not implemented yet")
                    is PartiqlAst.PathStep.PathWildcard -> TODO("PathWildcard is not implemented yet")
                }
                pathComponent.withStaticType(currentType)
            }
            // When [this](https://github.com/partiql/partiql-ir-generator/pull/53) is merged the below
            // can be simplified.
            return PartiqlAst.build {
                path(
                    root = path.root,
                    steps = newComponents,
                    metas = super.transformMetas(node.metas)
                ).withStaticType(currentType)
            }
        }

        private fun inferPathComponentExprType(
            previousComponentType: StaticType,
            currentPathComponent: PartiqlAst.PathStep.PathExpr
        ): StaticType =
            when (previousComponentType) {
                is AnyType -> StaticType.ANY
                is StructType -> inferStructLookupType(currentPathComponent, previousComponentType.fields, previousComponentType.contentClosed)
                is ListType,
                is SexpType -> {
                    val previous = previousComponentType as CollectionType // help Kotlin's type inference to be more specific
                    if (currentPathComponent.index.getStaticType() is IntType) {
                        previous.elementType
                    } else {
                        StaticType.MISSING
                    }
                }
                is AnyOfType -> {
                    when (previousComponentType.types.size) {
                        0 -> throw IllegalStateException("Cannot path on an empty StaticType union")
                        else -> {
                            val prevTypes = previousComponentType.allTypes
                            if (prevTypes.any { it is AnyType }) {
                                StaticType.ANY
                            } else {
                                val staticTypes = prevTypes.map { inferPathComponentExprType(it, currentPathComponent) }
                                AnyOfType(staticTypes.toSet()).flatten()
                            }
                        }
                    }
                }
                else -> StaticType.MISSING
            }

        private fun inferStructLookupType(
            currentPathComponent: PartiqlAst.PathStep.PathExpr,
            structFields: Map<String, StaticType>,
            contentClosed: Boolean
        ): StaticType =
            when (currentPathComponent.index) {
                is PartiqlAst.Expr.Lit -> {
                    if (currentPathComponent.index.value is StringElement) {
                        val bindings = Bindings.ofMap(structFields)
                        val caseSensitivity = currentPathComponent.case
                        val lookupName = BindingName(
                            currentPathComponent.index.value.stringValue,
                            caseSensitivity.toBindingCase()
                        )
                        bindings[lookupName] ?: if (contentClosed) {
                            StaticType.MISSING
                        } else {
                            StaticType.ANY
                        }
                    } else {
                        // Should this branch result in an error?
                        StaticType.MISSING
                    }
                }
                else -> {
                    StaticType.MISSING
                }
            }

        override fun transformLetBinding(node: PartiqlAst.LetBinding): PartiqlAst.LetBinding {
            val binding = super.transformLetBinding(node)
            addLocal(binding.name.text, binding.expr.getStaticType())
            return binding
        }

        override fun transformFromSourceScan_expr(node: PartiqlAst.FromSource.Scan): PartiqlAst.Expr {
            this.scopeOrder = ScopeSearchOrder.GLOBALS_THEN_LEXICAL
            return transformExpr(node.expr).also {
                this.scopeOrder = ScopeSearchOrder.LEXICAL
            }
        }

        override fun transformFromSourceUnpivot_expr(node: PartiqlAst.FromSource.Unpivot): PartiqlAst.Expr {
            this.scopeOrder = ScopeSearchOrder.GLOBALS_THEN_LEXICAL
            return transformExpr(node.expr).also {
                this.scopeOrder = ScopeSearchOrder.LEXICAL
            }
        }

        override fun transformProjection(node: PartiqlAst.Projection): PartiqlAst.Projection {
            val newProjection = super.transformProjection(node)

            val projectionType: StaticType = when (newProjection) {
                is PartiqlAst.Projection.ProjectList -> {
                    val contentClosed = newProjection.projectItems.filterIsInstance<PartiqlAst.ProjectItem.ProjectAll>().all {
                        val exprType = it.expr.getStaticType() as? StructType
                            ?: TODO("Expected Struct type for PartiqlAst.ProjectItem.ProjectAll expr")
                        exprType.contentClosed
                    }

                    val projectionFields = mutableMapOf<String, StaticType>()
                    for (item in newProjection.projectItems) {
                        when (item) {
                            is PartiqlAst.ProjectItem.ProjectExpr -> {
                                val projectionAsName = item.asAlias?.text
                                    ?: error("No alias found for projection")
                                if (projectionFields.containsKey(projectionAsName)) {
                                    // Duplicate select-list-item aliases are not allowed.
                                    // Keeps the static type of the first alias
                                    handleDuplicateAliasesError(item.expr.metas.getSourceLocation())
                                } else {
                                    projectionFields[projectionAsName] = item.expr.getStaticType()
                                }
                            }
                            is PartiqlAst.ProjectItem.ProjectAll -> {
                                val exprType = item.expr.getStaticType() as? StructType
                                    ?: TODO("Expected Struct type for PartiqlAst.ProjectItem.ProjectAll expr")
                                projectionFields.putAll(exprType.fields)
                            }
                        }
                    }
                    // TODO: Make the name optional in StaticType
                    StructType(fields = projectionFields, contentClosed = contentClosed)
                }
                is PartiqlAst.Projection.ProjectStar -> error(
                    "Encountered a SelectListItemStar." +
                        " This wouldn't be the case if SelectStarVisitorTransform ran before this."
                )
                is PartiqlAst.Projection.ProjectValue -> newProjection.value.getStaticType()
                is PartiqlAst.Projection.ProjectPivot -> TODO("PartiqlAst.Projection.ProjectPivot is not implemented yet")
            }

            return newProjection.withStaticType(projectionType)
        }

        private fun createVisitorTransformForNestedScope(): VisitorTransform {
            return VisitorTransform(currentEnv, currentScopeDepth + 1)
        }

        override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
            // calling transformExprSelectEvaluationOrder avoids the infinite recursion that would happen
            // if we called transformExprSelect on the nested visitor transform.
            val newNode = createVisitorTransformForNestedScope().transformExprSelectEvaluationOrder(node)
                as PartiqlAst.Expr.Select

            val projectionType = newNode.project.metas.staticType?.type
                ?: error("Select project wasn't assigned a StaticTypeMeta for some reason")

            return newNode.withStaticType(BagType(projectionType))
        }

        override fun transformExprSelect_where(node: PartiqlAst.Expr.Select): PartiqlAst.Expr? {
            return when (val whereExpr = node.where?.let { transformExpr(it) }) {
                null -> whereExpr
                else -> {
                    // verify `WHERE` clause is bool. If it's unknown, gives a null or missing error. If it could never
                    // be a bool, gives an incompatible data type for expression error
                    verifyExpressionType(expr = whereExpr, expectedType = StaticType.BOOL)

                    // continuation type for `WHERE` clause will be [StaticType.BOOL] regardless if there's an error
                    whereExpr.withStaticType(StaticType.BOOL)
                }
            }
        }
    }

    override fun transformStatement(node: PartiqlAst.Statement): PartiqlAst.Statement =
        VisitorTransform(wrapBindings(Bindings.empty(), 1), 0)
            .transformStatement(node)

    private fun wrapBindings(bindings: Bindings<StaticType>, depth: Int): Bindings<TypeAndDepth> {
        return Bindings.over { name ->
            bindings[name]?.let { bind ->
                TypeAndDepth(bind, depth)
            }
        }
    }
}
