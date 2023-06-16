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
import org.partiql.lang.ast.passes.inference.isNullOrMissing
import org.partiql.lang.ast.passes.inference.isNumeric
import org.partiql.lang.ast.passes.inference.isText
import org.partiql.lang.ast.passes.inference.isUnknown
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.domains.staticType
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.errors.ProblemSeverity
import org.partiql.lang.errors.ProblemThrower
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.builtins.SCALAR_BUILTINS_DEFAULT
import org.partiql.lang.eval.delegate
import org.partiql.lang.eval.getStartingSourceLocationMeta
import org.partiql.lang.eval.impl.FunctionManager
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticTypeUtils.areStaticTypesComparable
import org.partiql.lang.types.StaticTypeUtils.getTypeDomain
import org.partiql.lang.types.StaticTypeUtils.isSubTypeOf
import org.partiql.lang.types.StaticTypeUtils.staticTypeFromExprValueType
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.types.UnknownArguments
import org.partiql.lang.types.toTypedOpParameter
import org.partiql.lang.util.cartesianProduct
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BoolType
import org.partiql.types.CollectionType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.NumberConstraint
import org.partiql.types.SexpType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType

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
//    customFunctionSignatures: List<FunctionSignature>,
    val customFunction: List<ExprFunction>,
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
//    private val allFunctions: Map<String, FunctionSignature> =
//        SCALAR_BUILTINS_DEFAULT.associate { it.signature.name to it.signature } + customFunctionSignatures.associateBy { it.name }
    private val allFunctions: List<ExprFunction> = SCALAR_BUILTINS_DEFAULT + customFunction

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

        private fun List<PartiqlAst.Expr>.getStaticType(): List<StaticType> = map { it.getStaticType() }

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

        /**
         * Gives [SemanticProblemDetails.IncompatibleDatatypesForOp] error when none of the non-unknown [operandsStaticType]'
         * types satisfy [operandTypeValidator]. Also gives [SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing]
         * error when one of the operands is an unknown. Returns true if none of these errors are added.
         */
        private fun hasValidOperandTypes(
            operandsStaticType: List<StaticType>,
            operandTypeValidator: (StaticType) -> Boolean,
            op: String,
            metas: MetaContainer
        ): Boolean {
            var hasValidOperands = true

            // check for an incompatible operand type
            if (operandsStaticType.any { operandStaticType -> !operandStaticType.isUnknown() && operandStaticType.allTypes.none(operandTypeValidator) }) {
                handleIncompatibleDataTypesForOpError(operandsStaticType, op, metas.getSourceLocation())
                hasValidOperands = false
            }

            // check for an unknown operand type
            if (operandsStaticType.any { operandStaticType -> operandStaticType.isUnknown() }) {
                handleExpressionAlwaysReturnsNullOrMissingError(metas.getSourceLocation())
                hasValidOperands = false
            }

            return hasValidOperands
        }

        /**
         * Returns true if all of the provided [argsStaticType] are comparable to each other and are not unknown. Otherwise,
         * returns false.
         *
         * If an operand is not comparable to another, the [SemanticProblemDetails.IncompatibleDatatypesForOp] error is
         * handled by [problemHandler]. If an operand is unknown, the
         * [SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing] error is handled by [problemHandler].
         *
         * TODO: consider if collection comparison semantics should be different (e.g. errors over warnings,
         *  more details in error message): https://github.com/partiql/partiql-lang-kotlin/issues/505
         */
        private fun operandsAreComparable(argsStaticType: List<StaticType>, op: String, metas: MetaContainer): Boolean {
            var hasValidOperands = true

            // check for comparability of all operands. currently only adds one data type mismatch error
            outerLoop@ for (i in argsStaticType.indices) {
                for (j in i + 1 until argsStaticType.size) {
                    if (!areStaticTypesComparable(argsStaticType[i], argsStaticType[j])) {
                        handleIncompatibleDataTypesForOpError(argsStaticType, op, metas.getSourceLocation())
                        hasValidOperands = false
                        break@outerLoop
                    }
                }
            }

            // check for an unknown operand type
            if (argsStaticType.any { operand -> operand.isUnknown() }) {
                handleExpressionAlwaysReturnsNullOrMissingError(metas.getSourceLocation())
                hasValidOperands = false
            }
            return hasValidOperands
        }

        // Unary Op: NOT, POS, MINUS
        override fun transformExprNot(node: PartiqlAst.Expr.Not): PartiqlAst.Expr {
            val processedNode = super.transformExprNot(node) as PartiqlAst.Expr.Not
            val argStaticType = processedNode.expr.getStaticType()

            return when (hasValidOperandTypes(listOf(argStaticType), { it is BoolType }, "NOT", processedNode.metas)) {
            true -> computeReturnTypeForUnary(argStaticType, ::inferNotOp)
            false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprPos(node: PartiqlAst.Expr.Pos): PartiqlAst.Expr {
            val processedNode = super.transformExprPos(node) as PartiqlAst.Expr.Pos
            val argStaticType = processedNode.expr.getStaticType()

            return when (hasValidOperandTypes(listOf(argStaticType), { it.isNumeric() }, "+", processedNode.metas)) {
            true -> computeReturnTypeForUnary(argStaticType, ::inferUnaryArithmeticOp)
            false -> StaticType.NUMERIC // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprNeg(node: PartiqlAst.Expr.Neg): PartiqlAst.Expr {
            val processedNode = super.transformExprNeg(node) as PartiqlAst.Expr.Neg
            val argStaticType = processedNode.expr.getStaticType()

            return when (hasValidOperandTypes(listOf(argStaticType), { it.isNumeric() }, "-", processedNode.metas)) {
            true -> computeReturnTypeForUnary(argStaticType, ::inferUnaryArithmeticOp)
            false -> StaticType.NUMERIC // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        private fun computeReturnTypeForUnary(
            argStaticType: StaticType,
            unaryOpInferencer: (SingleType) -> SingleType
        ): StaticType {
            val argSingleTypes = argStaticType.allTypes.map { it as SingleType }
            val possibleReturnTypes = argSingleTypes.map { st -> unaryOpInferencer(st) }

            return StaticType.unionOf(possibleReturnTypes.toSet()).flatten()
        }

        private fun inferNotOp(type: SingleType): SingleType = when (type) {
            // Propagate NULL or MISSING
            is NullType -> StaticType.NULL
            is MissingType -> StaticType.MISSING
            is BoolType -> type
            else -> StaticType.MISSING
        }

        private fun inferUnaryArithmeticOp(type: SingleType): SingleType = when (type) {
            // Propagate NULL or MISSING
            is NullType -> StaticType.NULL
            is MissingType -> StaticType.MISSING
            is DecimalType, is IntType, is FloatType -> type
            else -> StaticType.MISSING
        }

        // Logical NAry ops: AND, OR
        // AND, OR operators are not like other NAry operators, NULL & MISSING don't simply propagate for them. That's
        // why we here we deal with them differently
        override fun transformExprAnd(node: PartiqlAst.Expr.And): PartiqlAst.Expr {
            val processedNode = super.transformExprAnd(node) as PartiqlAst.Expr.And
            return inferNaryLogicalOp(processedNode, processedNode.operands.getStaticType(), "AND")
        }

        override fun transformExprOr(node: PartiqlAst.Expr.Or): PartiqlAst.Expr {
            val processedNode = super.transformExprOr(node) as PartiqlAst.Expr.Or
            return inferNaryLogicalOp(processedNode, processedNode.operands.getStaticType(), "OR")
        }

        private fun inferNaryLogicalOp(node: PartiqlAst.Expr, argsStaticType: List<StaticType>, opAlias: String): PartiqlAst.Expr = when (hasValidOperandTypes(argsStaticType, { it is BoolType }, opAlias, node.metas)) {
        true -> {
            val argsSingleTypes = argsStaticType.map { argStaticType ->
                argStaticType.allTypes.map { singleType -> singleType as SingleType }
            }
            val argsSingleTypeCombination = argsSingleTypes.cartesianProduct()
            val possibleResultTypes = argsSingleTypeCombination.map { argsSingleType ->
                getTypeForNAryLogicalOperations(argsSingleType)
            }.toSet()

            StaticType.unionOf(possibleResultTypes).flatten()
        }
        false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
    }.let { node.withStaticType(it) }

        private fun getTypeForNAryLogicalOperations(args: List<SingleType>): StaticType = when {
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

        // NAry ops: ADD, SUB, MUL, DIV, MOD, CONCAT, EQ, NE, LT, LTE, GT, GTE
        override fun transformExprPlus(node: PartiqlAst.Expr.Plus): PartiqlAst.Expr {
            val processedNode = super.transformExprPlus(node) as PartiqlAst.Expr.Plus
            val argsStaticType = processedNode.operands.getStaticType()

            return when (hasValidOperandTypes(argsStaticType, { it.isNumeric() }, "+", processedNode.metas)) {
            true -> computeReturnTypeForNAry(argsStaticType, ::inferBinaryArithmeticOp)
            false -> StaticType.NUMERIC // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprMinus(node: PartiqlAst.Expr.Minus): PartiqlAst.Expr {
            val processedNode = super.transformExprMinus(node) as PartiqlAst.Expr.Minus
            val argsStaticType = processedNode.operands.getStaticType()

            return when (hasValidOperandTypes(argsStaticType, { it.isNumeric() }, "-", processedNode.metas)) {
            true -> computeReturnTypeForNAry(argsStaticType, ::inferBinaryArithmeticOp)
            false -> StaticType.NUMERIC // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprTimes(node: PartiqlAst.Expr.Times): PartiqlAst.Expr {
            val processedNode = super.transformExprTimes(node) as PartiqlAst.Expr.Times
            val argsStaticType = processedNode.operands.getStaticType()

            return when (hasValidOperandTypes(argsStaticType, { it.isNumeric() }, "*", processedNode.metas)) {
            true -> computeReturnTypeForNAry(argsStaticType, ::inferBinaryArithmeticOp)
            false -> StaticType.NUMERIC // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprDivide(node: PartiqlAst.Expr.Divide): PartiqlAst.Expr {
            val processedNode = super.transformExprDivide(node) as PartiqlAst.Expr.Divide
            val argsStaticType = processedNode.operands.getStaticType()

            return when (hasValidOperandTypes(argsStaticType, { it.isNumeric() }, "/", processedNode.metas)) {
            true -> computeReturnTypeForNAry(argsStaticType, ::inferBinaryArithmeticOp)
            false -> StaticType.NUMERIC // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprModulo(node: PartiqlAst.Expr.Modulo): PartiqlAst.Expr {
            val processedNode = super.transformExprModulo(node) as PartiqlAst.Expr.Modulo
            val argsStaticType = processedNode.operands.getStaticType()

            return when (hasValidOperandTypes(argsStaticType, { it.isNumeric() }, "%", processedNode.metas)) {
            true -> computeReturnTypeForNAry(argsStaticType, ::inferBinaryArithmeticOp)
            false -> StaticType.NUMERIC // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprConcat(node: PartiqlAst.Expr.Concat): PartiqlAst.Expr {
            val processedNode = super.transformExprConcat(node) as PartiqlAst.Expr.Concat
            val argsStaticType = processedNode.operands.getStaticType()

            return when (hasValidOperandTypes(argsStaticType, { it.isText() }, "||", processedNode.metas)) {
            true -> computeReturnTypeForNAry(argsStaticType, ::inferConcatOp)
            false -> StaticType.STRING // continuation type to prevent incompatible types and unknown errors from propagating
        }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprEq(node: PartiqlAst.Expr.Eq): PartiqlAst.Expr {
            val processedNode = super.transformExprEq(node) as PartiqlAst.Expr.Eq
            val argsStaticType = processedNode.operands.getStaticType()

            return when (operandsAreComparable(argsStaticType, "=", processedNode.metas)) {
                true -> computeReturnTypeForNAry(argsStaticType, ::inferEqNeOp)
                false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
            }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprNe(node: PartiqlAst.Expr.Ne): PartiqlAst.Expr {
            val processedNode = super.transformExprNe(node) as PartiqlAst.Expr.Ne
            val argsStaticType = processedNode.operands.getStaticType()

            return when (operandsAreComparable(argsStaticType, "!=", processedNode.metas)) {
                true -> computeReturnTypeForNAry(argsStaticType, ::inferEqNeOp)
                false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
            }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprGt(node: PartiqlAst.Expr.Gt): PartiqlAst.Expr {
            val processedNode = super.transformExprGt(node) as PartiqlAst.Expr.Gt
            val argsStaticType = processedNode.operands.getStaticType()

            return when (operandsAreComparable(argsStaticType, ">", processedNode.metas)) {
                true -> computeReturnTypeForNAry(argsStaticType, ::inferComparatorOp)
                false -> StaticType.BOOL // continuation type prevent incompatible types and unknown errors from propagating
            }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprGte(node: PartiqlAst.Expr.Gte): PartiqlAst.Expr {
            val processedNode = super.transformExprGte(node) as PartiqlAst.Expr.Gte
            val argsStaticType = processedNode.operands.getStaticType()

            return when (operandsAreComparable(argsStaticType, ">=", processedNode.metas)) {
                true -> computeReturnTypeForNAry(argsStaticType, ::inferComparatorOp)
                false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
            }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprLt(node: PartiqlAst.Expr.Lt): PartiqlAst.Expr {
            val processedNode = super.transformExprLt(node) as PartiqlAst.Expr.Lt
            val argsStaticType = processedNode.operands.getStaticType()

            return when (operandsAreComparable(argsStaticType, "<", processedNode.metas)) {
                true -> computeReturnTypeForNAry(argsStaticType, ::inferComparatorOp)
                false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
            }.let { processedNode.withStaticType(it) }
        }

        override fun transformExprLte(node: PartiqlAst.Expr.Lte): PartiqlAst.Expr {
            val processedNode = super.transformExprLte(node) as PartiqlAst.Expr.Lte
            val argsStaticType = processedNode.operands.getStaticType()

            return when (operandsAreComparable(argsStaticType, "<=", processedNode.metas)) {
                true -> computeReturnTypeForNAry(argsStaticType, ::inferComparatorOp)
                false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
            }.let { processedNode.withStaticType(it) }
        }

        private fun computeReturnTypeForNAry(
            argsStaticType: List<StaticType>,
            binaryOpInferencer: (SingleType, SingleType) -> SingleType,
        ): StaticType =
            argsStaticType.reduce { leftStaticType, rightStaticType ->
                val leftSingleTypes = leftStaticType.allTypes.map { it as SingleType }
                val rightSingleTypes = rightStaticType.allTypes.map { it as SingleType }
                val possibleResultTypes: List<SingleType> =
                    leftSingleTypes.flatMap { leftSingleType ->
                        rightSingleTypes.map { rightSingleType ->
                            binaryOpInferencer(leftSingleType, rightSingleType)
                        }
                    }

                StaticType.unionOf(possibleResultTypes.toSet()).flatten()
            }

        // This could also have been a lookup table of types, however... doing this as a nested `when` allows
        // us to not to rely on `.equals` and `.hashcode` implementations of [StaticType], which include metas
        // and might introduce unwanted behavior.
        private fun inferBinaryArithmeticOp(leftType: SingleType, rightType: SingleType): SingleType = when {
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

        private fun inferConcatOp(leftType: SingleType, rightType: SingleType): SingleType {
            fun checkUnconstrainedText(type: SingleType) = type is SymbolType || type is StringType && type.lengthConstraint is StringType.StringLengthConstraint.Unconstrained

            return when {
                // Propagate missing as missing. Missing has precedence over null
                leftType is MissingType || rightType is MissingType -> StaticType.MISSING
                leftType is NullType || rightType is NullType -> StaticType.NULL
                !leftType.isText() || !rightType.isText() -> StaticType.MISSING
                checkUnconstrainedText(leftType) || checkUnconstrainedText(rightType) -> StaticType.STRING
                else -> { // Constrained string types (char & varchar)
                    val leftLength = ((leftType as StringType).lengthConstraint as StringType.StringLengthConstraint.Constrained).length
                    val rightLength = ((rightType as StringType).lengthConstraint as StringType.StringLengthConstraint.Constrained).length
                    val sum = leftLength.value + rightLength.value
                    val newConstraint = when {
                        leftLength is NumberConstraint.UpTo || rightLength is NumberConstraint.UpTo -> NumberConstraint.UpTo(sum)
                        else -> NumberConstraint.Equals(sum)
                    }
                    StringType(StringType.StringLengthConstraint.Constrained(newConstraint))
                }
            }
        }

        // LT, LTE, GT, GTE
        private fun inferComparatorOp(lhs: SingleType, rhs: SingleType): SingleType = when {
            // Propagate missing as missing. Missing has precedence over null
            lhs is MissingType || rhs is MissingType -> StaticType.MISSING
            lhs is NullType || rhs is NullType -> StaticType.NULL
            areStaticTypesComparable(lhs, rhs) -> StaticType.BOOL
            else -> StaticType.MISSING
        }

        // EQ, NE
        private fun inferEqNeOp(lhs: SingleType, rhs: SingleType): SingleType = when {
            // Propagate missing as missing. Missing has precedence over null
            lhs is MissingType || rhs is MissingType -> StaticType.MISSING
            lhs.isNullable() || rhs.isNullable() -> StaticType.NULL
            else -> StaticType.BOOL
        }

        // Other Special NAry ops
        // BETWEEN Op
        override fun transformExprBetween(node: PartiqlAst.Expr.Between): PartiqlAst.Expr {
            val processedNode = super.transformExprBetween(node) as PartiqlAst.Expr.Between
            val argTypes = listOf(processedNode.value, processedNode.from, processedNode.to).getStaticType()
            if (!operandsAreComparable(argTypes, "BETWEEN", processedNode.metas)) {
                return processedNode.withStaticType(StaticType.BOOL)
            }

            val argsAllTypes = argTypes.map { it.allTypes }
            val possibleReturnTypes: MutableSet<SingleType> = mutableSetOf()

            argsAllTypes.cartesianProduct().forEach { argsChildType ->
                val argsSingleType = argsChildType.map { it as SingleType }
                when {
                    // If any one of the operands is null or missing, return NULL
                    argsSingleType.any { it is NullType || it is MissingType } -> possibleReturnTypes.add(StaticType.NULL)
                    areStaticTypesComparable(argsSingleType[0], argsSingleType[1]) || areStaticTypesComparable(argsSingleType[0], argsSingleType[2]) -> possibleReturnTypes.add(StaticType.BOOL)
                    else -> possibleReturnTypes.add(StaticType.MISSING)
                }
            }

            return processedNode.withStaticType(StaticType.unionOf(possibleReturnTypes).flatten())
        }

        // IN NAry op
        override fun transformExprInCollection(node: PartiqlAst.Expr.InCollection): PartiqlAst.Expr {
            val processedNode = super.transformExprInCollection(node) as PartiqlAst.Expr.InCollection
            val operands = processedNode.operands.getStaticType()
            val lhs = operands[0]
            val rhs = operands[1]
            var errorAdded = false

            // check if any operands are unknown, then null or missing error
            if (operands.any { operand -> operand.isUnknown() }) {
                handleExpressionAlwaysReturnsNullOrMissingError(processedNode.metas.getSourceLocation())
                errorAdded = true
            }

            // if none of the [rhs] types are [CollectionType]s with comparable element types to [lhs], then data type
            // mismatch error
            if (!rhs.isUnknown() && rhs.allTypes.none { it is CollectionType && areStaticTypesComparable(it.elementType, lhs) }) {
                handleIncompatibleDataTypesForOpError(operands, "IN", processedNode.metas.getSourceLocation())
                errorAdded = true
            }

            return when (errorAdded) {
                true -> StaticType.BOOL
                false -> computeReturnTypeForNAryIn(operands)
            }.let { processedNode.withStaticType(it) }
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

        // LIKE NAry op
        override fun transformExprLike(node: PartiqlAst.Expr.Like): PartiqlAst.Expr {
            val processedNode = super.transformExprLike(node) as PartiqlAst.Expr.Like
            val args = listOfNotNull(processedNode.value, processedNode.pattern, processedNode.escape)

            if (!hasValidOperandTypes(args.getStaticType(), { it.isText() }, "LIKE", processedNode.metas)) {
            return processedNode.withStaticType(StaticType.BOOL)
        }

            val argTypes = args.getStaticType()
            val argsAllTypes = argTypes.map { it.allTypes }
            val possibleReturnTypes: MutableSet<SingleType> = mutableSetOf()

            argsAllTypes.cartesianProduct().forEach { argsChildType ->
                val argsSingleType = argsChildType.map { it as SingleType }
                when {
                    // If any one of the operands is null, return NULL
                    argsSingleType.any { it is NullType } -> possibleReturnTypes.add(StaticType.NULL)
                    // Arguments for LIKE need to be text type
                    argsSingleType.all { it.isText() } -> {
                        possibleReturnTypes.add(StaticType.BOOL)
                        // If the optional escape character is provided, it can result in failure even if the type is text (string, in this case)
                        // This is because the escape character needs to be a single character (string with length 1),
                        // Even if the escape character is of length 1, escape sequence can be incorrect.
                        if (processedNode.escape != null) {
                            possibleReturnTypes.add(StaticType.MISSING)
                        }
                    }
                    else -> possibleReturnTypes.add(StaticType.MISSING)
                }
            }

            return processedNode.withStaticType(StaticType.unionOf(possibleReturnTypes).flatten())
        }

        // CALL
        override fun transformExprCall(node: PartiqlAst.Expr.Call): PartiqlAst.Expr {
            val processedNode = super.transformExprCall(node) as PartiqlAst.Expr.Call

            val arguments = processedNode.args
            val functionName = processedNode.funcName.text
            val arity = arguments.size
            val location = processedNode.metas.getSourceLocation()
            val functionManager = FunctionManager(allFunctions)
            var functions = functionManager.functionMap[functionName]
            if (functions == null) {
                handleNoSuchFunctionError(functionName, location)
                return processedNode.withStaticType(StaticType.ANY)
            }
            val funcsMatchingArity = functions.filter { it.signature.arity.contains(arity) }
            if (funcsMatchingArity.isEmpty()) {
                handleIncorrectNumberOfArgumentsToFunctionCallError(functionName, getMinMaxArities(functions).first..getMinMaxArities(functions).second, arity, location)
            } else {
                functions = funcsMatchingArity
            }

            for (func in functions) {
                return processedNode.withStaticType(
                    when (func.signature.unknownArguments) {
                        UnknownArguments.PROPAGATE -> returnTypeForPropagatingFunction(func.signature, arguments)
                        UnknownArguments.PASS_THRU -> returnTypeForPassThruFunction(func.signature, arguments)
                    }
                )
            }

            return processedNode
        }

        fun getMinMaxArities(funcs: List<ExprFunction>): Pair<Int, Int> {
            var minArity = Int.MAX_VALUE
            var maxArity = Int.MIN_VALUE

            funcs.forEach { func ->
                val currentArityMin = func.signature.arity.first
                val currentArityMax = func.signature.arity.last
                if (currentArityMin < minArity) minArity = currentArityMin
                if (currentArityMax > maxArity) maxArity = currentArityMax
            }

            return Pair(minArity, maxArity)
        }

        // Call agg : "count", "avg", "max", "min", "sum"
        override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr {
            val processedNode = super.transformExprCallAgg(node) as PartiqlAst.Expr.CallAgg
            val funcName = processedNode.funcName
            // unwrap the type if this is a collectionType
            val argType = when (val type = processedNode.arg.getStaticType()) {
                is CollectionType -> type.elementType
                else -> type
            }
            val sourceLocation = processedNode.getStartingSourceLocationMeta()
            return processedNode.withStaticType(computeReturnTypeForAggFunc(funcName.text, argType, sourceLocation))
        }

        private fun handleInvalidInputTypeForAggFun(sourceLocation: SourceLocationMeta, funcName: String, actualType: StaticType, expectedType: StaticType) {
            problemHandler.handleProblem(
                Problem(
                    sourceLocation = sourceLocation,
                    details = SemanticProblemDetails.InvalidArgumentTypeForFunction(
                        functionName = funcName,
                        expectedType = expectedType,
                        actualType = actualType
                    )
                )
            )
        }

        private fun computeReturnTypeForAggFunc(funcName: String, elementType: StaticType, sourceLocation: SourceLocationMeta): StaticType {
            val elementTypes = elementType.allTypes

            fun List<StaticType>.convertMissingToNull() = toMutableSet().apply {
                if (contains(StaticType.MISSING)) {
                    remove(StaticType.MISSING)
                    add(StaticType.NULL)
                }
            }

            fun StaticType.isUnknownOrNumeric() = isUnknown() || isNumeric()

            return when (funcName) {
                "count" -> StaticType.INT
                // In case that any element is MISSING or there is no element, we should return NULL
                "max", "min" -> StaticType.unionOf(elementTypes.convertMissingToNull())
                "sum" -> when {
                    elementTypes.none { it.isUnknownOrNumeric() } -> {
                        handleInvalidInputTypeForAggFun(sourceLocation, funcName, elementType, StaticType.unionOf(StaticType.NULL_OR_MISSING, StaticType.NUMERIC).flatten())
                        StaticType.unionOf(StaticType.NULL, StaticType.NUMERIC)
                    }
                    // If any single type is mismatched, We should add MISSING to the result types set to indicate there is a chance of data mismatch error
                    elementTypes.any { !it.isUnknownOrNumeric() } -> StaticType.unionOf(
                        elementTypes.filter { it.isUnknownOrNumeric() }.toMutableSet().apply { add(StaticType.MISSING) }
                    )
                    // In case that any element is MISSING or there is no element, we should return NULL
                    else -> StaticType.unionOf(elementTypes.convertMissingToNull())
                }
                // "avg" returns DECIMAL or NULL
                "avg" -> when {
                    elementTypes.none { it.isUnknownOrNumeric() } -> {
                        handleInvalidInputTypeForAggFun(sourceLocation, funcName, elementType, StaticType.unionOf(StaticType.NULL_OR_MISSING, StaticType.NUMERIC).flatten())
                        StaticType.unionOf(StaticType.NULL, StaticType.DECIMAL)
                    }
                    else -> StaticType.unionOf(
                        mutableSetOf<SingleType>().apply {
                            if (elementTypes.any { it.isUnknown() }) { add(StaticType.NULL) }
                            if (elementTypes.any { it.isNumeric() }) { add(StaticType.DECIMAL) }
                            // If any single type is mismatched, We should add MISSING to the result types set to indicate there is a chance of data mismatch error
                            if (elementTypes.any { !it.isUnknownOrNumeric() }) { add(StaticType.MISSING) }
                        }
                    )
                }
                else -> error("Internal Error: Unsupported aggregate function. This probably indicates a parser bug.")
            }.flatten()
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
                    if (getTypeDomain(actualNonUnknownType).intersect(getTypeDomain(expectedType)).isEmpty()) {
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
            val allArgs = requiredArgs

            return if (functionHasValidArgTypes(signature.name, allArgs)) {
                val finalReturnTypes = signature.returnType.allTypes + allArgs.flatMap { (actualExpr, expectedType) ->
                    val actualType = actualExpr.getStaticType()
                    listOfNotNull(
                        // if any type is `MISSING`, add `MISSING` to possible return types.
                        // if the actual type is not a subtype is the expected type, add `MISSING`. In the future, may
                        // want to give a warning that a data type mismatch could occur
                        // (https://github.com/partiql/partiql-lang-kotlin/issues/507)
                        StaticType.MISSING.takeIf {
                            actualType.allTypes.any { it is MissingType } || !isSubTypeOf(actualType.filterNullMissing(), expectedType)
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
                    getTypeDomain(actual.getStaticType()).intersect(getTypeDomain(expected)).isNotEmpty()
                }
            return requiredArgumentsMatch
        }

        /**
         * Function assumes the number of [arguments] passed agrees with the [signature]
         * Returns true when all the arguments (required, optional, variadic) are subtypes of the expected arguments for the [signature].
         * Returns false otherwise
         */
        private fun matchesAllArguments(arguments: List<PartiqlAst.Expr>, signature: FunctionSignature): Boolean {
            // Checks if the actual StaticType is subtype of expected StaticType ( filtering the null/missing for PROPAGATING functions
            fun isSubType(actual: StaticType, expected: StaticType): Boolean {
                val lhs = when (signature.unknownArguments) {
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
                return isSubTypeOf(lhs, expected)
            }

            val requiredArgumentsMatch = arguments
                .zip(signature.requiredParameters)
                .all { (actual, expected) ->
                    val st = actual.getStaticType()
                    isSubType(st, expected)
                }
            return requiredArgumentsMatch
        }

        override fun transformExprLit(node: PartiqlAst.Expr.Lit): PartiqlAst.Expr {
            val literal = super.transformExprLit(node) as PartiqlAst.Expr.Lit
            val exprValueType = ExprValueType.fromIonType(literal.value.type.toIonType())
            return literal.withStaticType(staticTypeFromExprValueType(exprValueType))
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
            val valuesTypes = AnyOfType(values.getStaticType().toSet()).flatten()
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
                else if (!areStaticTypesComparable(caseValueType, whenExprType)) {
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
            val thenExprsTypes = thenExprs.getStaticType()
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
            val targetType = typed.asType.toTypedOpParameter()
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
            operandsAreComparable(listOf(nullIf.expr1, nullIf.expr2).getStaticType(), "NULLIF", nullIf.metas)

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
            val structFields = mutableListOf<StructType.Field>()
            var closedContent = true
            struct.fields.forEach { expr ->
                val nameExpr = expr.first
                val valueExpr = expr.second
                when (nameExpr) {
                    is PartiqlAst.Expr.Lit ->
                        // A field is only included in the StructType if its key is a text literal
                        if (nameExpr.value is TextElement) {
                            structFields.add(StructType.Field(nameExpr.value.textValue, valueExpr.getStaticType()))
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
                .groupingBy { it.key }
                .eachCount()
                .any { it.value > 1 }

            if (hasDuplicateKeys) {
                TODO("Duplicate keys in struct is not yet handled")
            }

            return struct.withStaticType(StructType(structFields, contentClosed = closedContent))
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
                val hasLists = getTypeDomain(fromExprType).contains(ExprValueType.LIST)
                val hasOnlyLists = hasLists && (getTypeDomain(fromExprType).size == 1)
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
                    AnyOfType(fromSourceType.fields.map { it.value }.toSet()).flatten()
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
                val valueHasMissing = getTypeDomain(valueType).contains(ExprValueType.MISSING)
                val valueOnlyHasMissing = valueHasMissing && getTypeDomain(valueType).size == 1
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
            return path.copy(
                steps = newComponents,
                metas = super.transformMetas(node.metas)
            ).withStaticType(currentType)
        }

        private fun inferPathComponentExprType(
            previousComponentType: StaticType,
            currentPathComponent: PartiqlAst.PathStep.PathExpr
        ): StaticType =
            when (previousComponentType) {
                is AnyType -> StaticType.ANY
                is StructType -> inferStructLookupType(currentPathComponent, previousComponentType.fields.associate { it.key to it.value }, previousComponentType.contentClosed)
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
            when (val index = currentPathComponent.index) {
                is PartiqlAst.Expr.Lit -> {
                    if (index.value is StringElement) {
                        val bindings = Bindings.ofMap(structFields)
                        val caseSensitivity = currentPathComponent.case
                        val lookupName = BindingName(
                            index.value.stringValue,
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
            val type = when (newProjection) {
                is PartiqlAst.Projection.ProjectList -> {
                    val contentClosed = newProjection.projectItems.filterIsInstance<PartiqlAst.ProjectItem.ProjectAll>().all {
                        val exprType = it.expr.getStaticType() as? StructType
                            ?: TODO("Expected Struct type for PartiqlAst.ProjectItem.ProjectAll expr")
                        exprType.contentClosed
                    }
                    val projectionFields = mutableListOf<StructType.Field>()
                    for (item in newProjection.projectItems) {
                        when (item) {
                            is PartiqlAst.ProjectItem.ProjectExpr -> {
                                val projectionAsName = item.asAlias?.text
                                    ?: error("No alias found for projection")
                                if (projectionFields.find { it.key == projectionAsName } != null) {
                                    // Duplicate select-list-item aliases are not allowed.
                                    // Keeps the static type of the first alias
                                    handleDuplicateAliasesError(item.expr.metas.getSourceLocation())
                                } else {
                                    projectionFields.add(StructType.Field(projectionAsName, item.expr.getStaticType()))
                                }
                            }
                            is PartiqlAst.ProjectItem.ProjectAll -> {
                                val exprType = item.expr.getStaticType() as? StructType
                                    ?: TODO("Expected Struct type for PartiqlAst.ProjectItem.ProjectAll expr")
                                projectionFields.addAll(exprType.fields)
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
                is PartiqlAst.Projection.ProjectPivot -> StaticType.STRUCT
            }
            return newProjection.withStaticType(type)
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

            val selectType = when (newNode.project) {
                is PartiqlAst.Projection.ProjectList,
                is PartiqlAst.Projection.ProjectValue -> BagType(projectionType)
                is PartiqlAst.Projection.ProjectStar ->
                    error("expected SelectListItemStar transform to be ran before this")
                is PartiqlAst.Projection.ProjectPivot -> projectionType
            }

            return newNode.withStaticType(selectType)
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

    /** Helper to convert [PartiqlAst.Type] in AST to a [TypedOpParameter]. */
    private fun PartiqlAst.Type.toTypedOpParameter(): TypedOpParameter {
        // hack: to avoid duplicating the function `PartiqlAst.Type.toTypedOpParameter`, we have to convert this
        // PartiqlAst.Type to PartiqlPhysical.Type. The easiest way to do that without using a visitor transform
        // (which is overkill and comes with some downsides for something this simple), is to transform to and from
        // s-expressions again.  This will work without difficulty as long as PartiqlAst.Type remains unchanged in all
        // permuted domains between PartiqlAst and PartiqlPhysical.

        // This is really just a temporary measure, however, which must exist for as long as the type inferencer works only
        // on PartiqlAst.  When it has been migrated to use PartiqlPhysical instead, there should no longer be a reason
        // to keep this function around.
        val sexp = this.toIonElement()
        val physicalType = PartiqlPhysical.transform(sexp) as PartiqlPhysical.Type
        return physicalType.toTypedOpParameter(customTypedOpParameters)
    }
}
