/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.planner.transforms.plan

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.TextElement
import org.partiql.lang.ast.UNKNOWN_SOURCE_LOCATION
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.ast.passes.inference.cast
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.builtins.SCALAR_BUILTINS_DEFAULT
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.transforms.PlannerSession
import org.partiql.lang.planner.transforms.impl.Metadata
import org.partiql.lang.planner.transforms.plan.PlanTyper.MinimumTolerance.FULL
import org.partiql.lang.planner.transforms.plan.PlanTyper.MinimumTolerance.PARTIAL
import org.partiql.lang.planner.transforms.plan.PlanUtils.addType
import org.partiql.lang.planner.transforms.plan.PlanUtils.grabType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticTypeUtils
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.types.UnknownArguments
import org.partiql.lang.util.cartesianProduct
import org.partiql.plan.Arg
import org.partiql.plan.Binding
import org.partiql.plan.Case
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.plan.Property
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Step
import org.partiql.plan.visitor.PlanRewriter
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
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
import org.partiql.types.TupleConstraint

/**
 * Types a given logical plan.
 */
internal object PlanTyper : PlanRewriter<PlanTyper.Context>() {

    /**
     * Given a [Rex], types the logical plan by adding the output Type Environment to each relational operator.
     *
     * Along with typing, this also validates expressions for typing issues.
     */
    internal fun type(node: Rex, ctx: Context): Rex {
        return visitRex(node, ctx) as Rex
    }

    /**
     * Used for maintaining state through the visitors
     */
    internal class Context(
        internal val input: Rel?,
        internal val session: PlannerSession,
        internal val metadata: Metadata,
        internal val scopingOrder: ScopingOrder,
        internal val customFunctionSignatures: List<FunctionSignature>,
        internal val tolerance: MinimumTolerance = MinimumTolerance.FULL,
        internal val problemHandler: ProblemHandler
    ) {
        internal val inputTypeEnv = input?.let { PlanUtils.getTypeEnv(it) } ?: emptyList()
        internal val allFunctions = SCALAR_BUILTINS_DEFAULT.associate { it.signature.name to it.signature } + customFunctionSignatures.associateBy { it.name }
    }

    /**
     * Scoping
     */
    internal enum class ScopingOrder {
        GLOBALS_THEN_LEXICAL,
        LEXICAL_THEN_GLOBALS
    }

    /**
     * [FULL] -- CANNOT tolerate references to unresolved variables
     * [PARTIAL] -- CAN tolerate references to unresolved variables
     */
    internal enum class MinimumTolerance {
        FULL,
        PARTIAL
    }

    //
    //
    // RELATIONAL ALGEBRA OPERATORS
    //
    //

    override fun visitRelBag(node: Rel.Bag, ctx: Context): PlanNode {
        TODO("BAG OPERATORS are not supported by the PartiQLTypeEnvInferencer yet.")
    }

    override fun visitRel(node: Rel, ctx: Context): Rel = super.visitRel(node, ctx) as Rel

    override fun visitRelJoin(node: Rel.Join, ctx: Context): Rel.Join {
        val lhs = visitRel(node.lhs, ctx)
        val rhs = typeRel(node.rhs, lhs, ctx)
        val newJoin = node.copy(
            common = node.common.copy(
                typeEnv = lhs.getTypeEnv() + rhs.getTypeEnv(),
            )
        )
        val predicateType = when (val condition = node.condition) {
            null -> StaticType.BOOL
            else -> {
                val predicate = typeRex(condition, newJoin, ctx)
                // verify `JOIN` predicate is bool. If it's unknown, gives a null or missing error. If it could
                // never be a bool, gives an incompatible data type for expression error
                assertType(expected = StaticType.BOOL, actual = predicate.grabType() ?: handleMissingType(ctx), ctx)

                // continuation type (even in the case of an error) is [StaticType.BOOL]
                StaticType.BOOL
            }
        }
        return newJoin.copy(
            condition = node.condition?.addType(predicateType)
        )
    }

    override fun visitRelUnpivot(node: Rel.Unpivot, ctx: Context): Rel.Unpivot {
        val from = node

        val asSymbolicName = node.alias
            ?: error("Unpivot alias is null.  This wouldn't be the case if FromSourceAliasVisitorTransform was executed first.")

        val value = visitRex(from.value, ctx) as Rex

        val fromExprType = value.grabType() ?: handleMissingType(ctx)

        val valueType = getUnpivotValueType(fromExprType)
        val typeEnv = mutableListOf(Plan.attribute(asSymbolicName, valueType))

        from.at?.let {
            val valueHasMissing = StaticTypeUtils.getTypeDomain(valueType).contains(ExprValueType.MISSING)
            val valueOnlyHasMissing = valueHasMissing && StaticTypeUtils.getTypeDomain(valueType).size == 1
            when {
                valueOnlyHasMissing -> {
                    typeEnv.add(Plan.attribute(it, StaticType.MISSING))
                }
                valueHasMissing -> {
                    typeEnv.add(Plan.attribute(it, StaticType.STRING.asOptional()))
                }
                else -> {
                    typeEnv.add(Plan.attribute(it, StaticType.STRING))
                }
            }
        }

        node.by?.let { TODO("BY variable's inference is not implemented yet.") }

        return from.copy(
            common = from.common.copy(
                typeEnv = typeEnv
            ),
            value = value
        )
    }

    override fun visitRelAggregate(node: Rel.Aggregate, ctx: Context): PlanNode {
        val input = visitRel(node.input, ctx)
        val calls = node.calls.map { Plan.binding(it.name, typeRex(it.value, input, ctx)) }
        val groups = node.groups.map { Plan.binding(it.name, typeRex(it.value, input, ctx)) }
        return node.copy(
            calls = calls,
            groups = groups,
            common = node.common.copy(
                typeEnv = groups.toAttributes(ctx) + calls.toAttributes(ctx)
            )
        )
    }

    override fun visitRelProject(node: Rel.Project, ctx: Context): PlanNode {
        val input = visitRel(node.input, ctx)
        val typeEnv = node.bindings.flatMap { binding ->
            val type = inferType(binding.value, input, ctx)
            when (binding.value.isProjectAll()) {
                true -> {
                    when (val structType = type as? StructType) {
                        null -> {
                            handleIncompatibleDataTypeForExprError(StaticType.STRUCT, type, ctx)
                            listOf(Plan.attribute(binding.name, type))
                        }
                        else -> structType.fields.map { entry -> Plan.attribute(entry.key, entry.value) }
                    }
                }
                false -> listOf(Plan.attribute(binding.name, type))
            }
        }
        return node.copy(
            input = input,
            common = node.common.copy(
                typeEnv = typeEnv
            )
        )
    }

    override fun visitRelScan(node: Rel.Scan, ctx: Context): Rel {
        val value = visitRex(
            node.value,
            Context(
                ctx.input,
                ctx.session,
                ctx.metadata,
                ScopingOrder.GLOBALS_THEN_LEXICAL,
                ctx.customFunctionSignatures,
                ctx.tolerance,
                ctx.problemHandler
            )
        ) as Rex
        val asSymbolicName = node.alias ?: error("From Source Alias is null when it should not be.")
        val valueType = value.grabType() ?: handleMissingType(ctx)
        val sourceType = getElementTypeForFromSource(valueType)

        node.at?.let { TODO("AT is not supported yet.") }
        node.by?.let { TODO("BY is not supported yet.") }

        return when (value) {
            is Rex.Query.Collection -> when (value.constructor) {
                null -> value.rel
                else -> {
                    val typeEnv = listOf(Plan.attribute(asSymbolicName, sourceType))
                    node.copy(
                        value = value,
                        common = node.common.copy(
                            typeEnv = typeEnv
                        )
                    )
                }
            }
            else -> {
                val typeEnv = listOf(Plan.attribute(asSymbolicName, sourceType))
                node.copy(
                    value = value,
                    common = node.common.copy(
                        typeEnv = typeEnv
                    )
                )
            }
        }
    }

    override fun visitRelFilter(node: Rel.Filter, ctx: Context): PlanNode {
        val input = visitRel(node.input, ctx)
        val condition = typeRex(node.condition, input, ctx)
        assertType(StaticType.BOOL, condition.grabType() ?: handleMissingType(ctx), ctx)
        return node.copy(
            condition = condition,
            input = input,
            common = node.common.copy(
                typeEnv = input.getTypeEnv(),
                properties = input.getProperties()
            )
        )
    }

    override fun visitRelSort(node: Rel.Sort, ctx: Context): PlanNode {
        val input = visitRel(node.input, ctx)
        return node.copy(
            input = input,
            common = node.common.copy(
                typeEnv = input.getTypeEnv(),
                properties = setOf(Property.ORDERED)
            )
        )
    }

    override fun visitRelFetch(node: Rel.Fetch, ctx: Context): PlanNode {
        val input = visitRel(node.input, ctx)
        val limit = typeRex(node.limit, input, ctx)
        val offset = typeRex(node.offset, input, ctx)
        limit.grabType()?.let { assertAsInt(it, ctx) }
        offset.grabType()?.let { assertAsInt(it, ctx) }
        return node.copy(
            input = input,
            common = node.common.copy(
                typeEnv = input.getTypeEnv(),
                properties = input.getProperties()
            ),
            limit = limit,
            offset = offset
        )
    }

    //
    //
    // EXPRESSIONS
    //
    //

    override fun visitRexQueryScalarPivot(node: Rex.Query.Scalar.Pivot, ctx: Context): PlanNode {
        // TODO: This is to match the StaticTypeInferenceVisitorTransform logic, but needs to be changed
        return node.copy(
            type = StaticType.STRUCT
        )
    }

    override fun visitRexQueryScalarSubquery(node: Rex.Query.Scalar.Subquery, ctx: Context): PlanNode {
        val query = visitRex(node.query, ctx) as Rex.Query.Collection
        when (val queryType = query.grabType() ?: handleMissingType(ctx)) {
            is CollectionType -> queryType.elementType
            else -> error("Query collection subqueries should always return a CollectionType.")
        }
        return node.copy(
            query = query
        )
    }

    override fun visitRex(node: Rex, ctx: Context): PlanNode = super.visitRex(node, ctx)

    override fun visitRexAgg(node: Rex.Agg, ctx: Context): PlanNode {
        val funcName = node.id
        val args = node.args.map { visitRex(it, ctx) as Rex }
        // unwrap the type if this is a collectionType
        val argType = when (val type = args[0].grabType() ?: handleMissingType(ctx)) {
            is CollectionType -> type.elementType
            else -> type
        }
        return node.copy(
            type = computeReturnTypeForAggFunc(funcName, argType, ctx),
            args = args
        )
    }

    private fun computeReturnTypeForAggFunc(funcName: String, elementType: StaticType, ctx: Context): StaticType {
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
                    handleInvalidInputTypeForAggFun(funcName, elementType, StaticType.unionOf(StaticType.NULL_OR_MISSING, StaticType.NUMERIC).flatten(), ctx)
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
                    handleInvalidInputTypeForAggFun(funcName, elementType, StaticType.unionOf(StaticType.NULL_OR_MISSING, StaticType.NUMERIC).flatten(), ctx)
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

    private fun handleInvalidInputTypeForAggFun(funcName: String, actualType: StaticType, expectedType: StaticType, ctx: Context) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.InvalidArgumentTypeForFunction(
                    functionName = funcName,
                    expectedType = expectedType,
                    actualType = actualType
                )
            )
        )
    }

    override fun visitRexQueryScalar(node: Rex.Query.Scalar, ctx: Context): PlanNode = super.visitRexQueryScalar(node, ctx)

    override fun visitRexQuery(node: Rex.Query, ctx: Context): PlanNode = super.visitRexQuery(node, ctx)

    override fun visitRexQueryCollection(node: Rex.Query.Collection, ctx: Context): PlanNode {
        val input = visitRel(node.rel, ctx)
        val typeConstructor = when (input.getProperties().contains(Property.ORDERED)) {
            true -> { type: StaticType -> ListType(type) }
            false -> { type: StaticType -> BagType(type) }
        }
        return when (val constructor = node.constructor) {
            null -> {
                node.copy(
                    rel = input,
                    type = typeConstructor.invoke(
                        StructType(
                            fields = input.getTypeEnv().map { attribute ->
                                StructType.Field(attribute.name, attribute.type)
                            },
                            contentClosed = true,
                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                        )
                    )
                )
            }
            else -> {
                val constructorType = typeRex(constructor, input, ctx).grabType() ?: handleMissingType(ctx)
                return node.copy(
                    type = typeConstructor.invoke(constructorType)
                )
            }
        }
    }

    override fun visitRexPath(node: Rex.Path, ctx: Context): Rex.Path {
        val ids = grabFirstIds(node)
        val qualifier = ids.getOrNull(0)?.qualifier ?: Rex.Id.Qualifier.UNQUALIFIED
        val path = BindingPath(ids.map { rexIdToBindingName(it) })
        val pathAndType = findBind(path, qualifier, ctx)
        val remainingFirstIndex = pathAndType.levelsMatched - 1
        val remaining = when (remainingFirstIndex > node.steps.lastIndex) {
            true -> emptyList()
            false -> node.steps.subList(remainingFirstIndex, node.steps.size)
        }
        var currentType = pathAndType.type
        remaining.forEach { pathComponent ->
            currentType = when (pathComponent) {
                is Step.Key -> {
                    val type = inferPathComponentExprType(currentType, pathComponent, ctx)
                    type
                }
                is Step.Wildcard -> currentType
                is Step.Unpivot -> error("Not implemented yet")
            }
        }
        return node.copy(
            type = currentType
        )
    }

    override fun visitRexId(node: Rex.Id, ctx: Context): Rex.Id {
        val bindingPath = BindingPath(listOf(rexIdToBindingName(node)))
        return node.copy(type = findBind(bindingPath, node.qualifier, ctx).type)
    }

    override fun visitRexBinary(node: Rex.Binary, ctx: Context): Rex.Binary {
        val lhs = visitRex(node.lhs, ctx).grabType() ?: handleMissingType(ctx)
        val rhs = visitRex(node.rhs, ctx).grabType() ?: handleMissingType(ctx)
        val args = listOf(lhs, rhs)
        val type = when (node.op) {
            Rex.Binary.Op.PLUS, Rex.Binary.Op.MINUS, Rex.Binary.Op.TIMES, Rex.Binary.Op.DIV, Rex.Binary.Op.MODULO -> when (hasValidOperandTypes(args, node.op.name, ctx) { it.isNumeric() }) {
                true -> computeReturnTypeForNAry(args, PlanTyper::inferBinaryArithmeticOp)
                false -> StaticType.NUMERIC // continuation type to prevent incompatible types and unknown errors from propagating
            }
            Rex.Binary.Op.CONCAT -> when (hasValidOperandTypes(args, node.op.name, ctx) { it.isText() }) {
                true -> computeReturnTypeForNAry(args, PlanTyper::inferConcatOp)
                false -> StaticType.STRING // continuation type to prevent incompatible types and unknown errors from propagating
            }
            Rex.Binary.Op.AND, Rex.Binary.Op.OR -> inferNaryLogicalOp(args, node.op.name, ctx)
            Rex.Binary.Op.EQ, Rex.Binary.Op.NEQ -> when (operandsAreComparable(args, node.op.name, ctx)) {
                true -> computeReturnTypeForNAry(args, PlanTyper::inferEqNeOp)
                false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
            }
            Rex.Binary.Op.LT, Rex.Binary.Op.GT, Rex.Binary.Op.LTE, Rex.Binary.Op.GTE -> when (operandsAreComparable(args, node.op.name, ctx)) {
                true -> computeReturnTypeForNAry(args, PlanTyper::inferComparatorOp)
                false -> StaticType.BOOL // continuation type prevent incompatible types and unknown errors from propagating
            }
        }
        return node.copy(type = type)
    }

    override fun visitRexUnary(node: Rex.Unary, ctx: Context): PlanNode {
        val valueType = visitRex(node.value, ctx).grabType() ?: handleMissingType(ctx)
        val type = when (node.op) {
            Rex.Unary.Op.NOT -> when (hasValidOperandTypes(listOf(valueType), node.op.name, ctx) { it is BoolType }) {
                true -> computeReturnTypeForUnary(valueType, PlanTyper::inferNotOp)
                false -> StaticType.BOOL // continuation type to prevent incompatible types and unknown errors from propagating
            }
            Rex.Unary.Op.POS -> when (hasValidOperandTypes(listOf(valueType), node.op.name, ctx) { it.isNumeric() }) {
                true -> computeReturnTypeForUnary(valueType, PlanTyper::inferUnaryArithmeticOp)
                false -> StaticType.NUMERIC
            }
            Rex.Unary.Op.NEG -> when (hasValidOperandTypes(listOf(valueType), node.op.name, ctx) { it.isNumeric() }) {
                true -> computeReturnTypeForUnary(valueType, PlanTyper::inferUnaryArithmeticOp)
                false -> StaticType.NUMERIC
            }
        }
        return node.copy(type = type)
    }

    // This type comes from RexConverter
    override fun visitRexLit(node: Rex.Lit, ctx: Context): Rex.Lit = node

    override fun visitRexCollection(node: Rex.Collection, ctx: Context): PlanNode = super.visitRexCollection(node, ctx)

    override fun visitRexCollectionArray(node: Rex.Collection.Array, ctx: Context): PlanNode {
        val typedValues = node.values.map { visitRex(it, ctx) as Rex }
        val elementType = AnyOfType(typedValues.map { it.grabType() ?: handleMissingType(ctx) }.toSet()).flatten()
        return node.copy(type = ListType(elementType), values = typedValues)
    }

    override fun visitRexCollectionBag(node: Rex.Collection.Bag, ctx: Context): PlanNode {
        val typedValues = node.values.map { visitRex(it, ctx) }
        val elementType = AnyOfType(typedValues.map { it.grabType()!! }.toSet()).flatten()
        return node.copy(type = BagType(elementType))
    }

    override fun visitRexCall(node: Rex.Call, ctx: Context): Rex.Call {
        val processedNode = processRexCall(node, ctx)
        visitRexCallManual(processedNode, ctx)?.let { return it }
        val funcName = node.id
        val signature = ctx.allFunctions[funcName]
        if (signature == null) {
            handleNoSuchFunctionError(ctx, funcName)
            return node.copy(type = StaticType.ANY)
        }
        val type = node.type ?: computeReturnTypeForFunctionCall(signature, processedNode.args.getTypes(ctx), ctx)
        return processedNode.copy(type = type)
    }

    override fun visitRexSwitch(node: Rex.Switch, ctx: Context): PlanNode {
        val match = node.match?.let { visitRex(it, ctx) as Rex }
        val caseValueType = when (match) {
            null -> null
            else -> {
                val type = match.grabType() ?: handleMissingType(ctx)
                // comparison never succeeds if caseValue is an unknown
                if (type.isUnknown()) {
                    handleExpressionAlwaysReturnsNullOrMissingError(ctx)
                }
                type
            }
        }
        val check = when (caseValueType) {
            null -> { conditionType: StaticType ->
                conditionType.allTypes.none { it is BoolType }
            }
            else -> { conditionType: StaticType ->
                !StaticTypeUtils.areStaticTypesComparable(caseValueType, conditionType)
            }
        }
        val branches = node.branches.map { branch ->
            val condition = visitRex(branch.condition, ctx) as Rex
            val value = visitRex(branch.value, ctx) as Rex
            val conditionType = condition.grabType() ?: handleMissingType(ctx)
            // comparison never succeeds if whenExpr is unknown -> null or missing error
            if (conditionType.isUnknown()) {
                handleExpressionAlwaysReturnsNullOrMissingError(ctx)
            }
            // if caseValueType is incomparable to whenExprType -> data type mismatch
            else if (check.invoke(conditionType)) {
                handleIncompatibleDataTypesForOpError(
                    ctx,
                    actualTypes = listOfNotNull(caseValueType, conditionType),
                    op = "CASE"
                )
            }
            branch.copy(condition = condition, value = value)
        }
        val valueTypes = branches.map { it.value }.map { it.grabType() ?: handleMissingType(ctx) }

        // keep all the `THEN` expr types even if the comparison doesn't succeed
        val default = node.default?.let { visitRex(it, ctx) }
        val type = inferCaseWhenBranches(valueTypes, default?.grabType())
        return node.copy(
            match = match,
            branches = branches,
            type = type
        )
    }

    override fun visitRexTuple(node: Rex.Tuple, ctx: Context): PlanNode {
        val fields = node.fields.map { field ->
            field.copy(
                name = visitRex(field.name, ctx) as Rex,
                value = visitRex(field.value, ctx) as Rex
            )
        }

        val structFields = mutableListOf<StructType.Field>()
        var closedContent = true
        fields.forEach { field ->
            when (val name = field.name) {
                is Rex.Lit ->
                    // A field is only included in the StructType if its key is a text literal
                    if (name.value is TextElement) {
                        val value = name.value as TextElement
                        val type = field.value.grabType() ?: handleMissingType(ctx)
                        structFields.add(StructType.Field(value.textValue, type))
                    }
                else -> {
                    // A field with a non-literal key name is not included.
                    // If the non-literal could be text, StructType will have open content.
                    val nameType = field.name.grabType() ?: handleMissingType(ctx)
                    if (nameType.allTypes.any { it.isText() }) {
                        closedContent = false
                    }
                }
            }
        }

        val hasDuplicateKeys = structFields
            .groupingBy { it.key }
            .eachCount()
            .any { it.value > 1 }

        return node.copy(
            type = StructType(
                structFields,
                contentClosed = closedContent,
                constraints = setOf(TupleConstraint.Open(closedContent.not()), TupleConstraint.UniqueAttrs(hasDuplicateKeys.not()))
            ),
            fields = fields
        )
    }

    override fun visitArgValue(node: Arg.Value, ctx: Context): PlanNode {
        return node.copy(
            value = visitRex(node.value, ctx) as Rex
        )
    }

    //
    //
    // HELPER METHODS
    //
    //

    private fun inferCaseWhenBranches(thenExprsTypes: List<StaticType>, elseExpr: StaticType?): StaticType {
        val elseExprType = when (elseExpr) {
            // If there is no ELSE clause in the expression, it possible that
            // none of the WHEN clauses succeed and the output of CASE WHEN expression
            // ends up being NULL
            null -> StaticType.NULL
            else -> elseExpr
        }

        if (thenExprsTypes.any { it is AnyType } || elseExprType is AnyType) {
            return StaticType.ANY
        }

        val possibleTypes = thenExprsTypes + elseExprType
        return AnyOfType(possibleTypes.toSet()).flatten()
    }

    /**
     * Assumes that [node] has been pre-processed.
     */
    private fun visitRexCallManual(node: Rex.Call, ctx: Context): Rex.Call? {
        return when (node.id) {
            RexConverter.Constants.inCollection -> visitRexCallInCollection(node, ctx)
            RexConverter.Constants.between -> visitRexCallBetween(node, ctx)
            RexConverter.Constants.like, RexConverter.Constants.likeEscape -> visitRexCallLike(node, ctx)
            RexConverter.Constants.canCast, RexConverter.Constants.canLosslessCast, RexConverter.Constants.isType -> node.copy(type = StaticType.BOOL)
            RexConverter.Constants.coalesce -> visitRexCallCoalesce(node, ctx)
            RexConverter.Constants.nullIf -> visitRexCallNullIf(node, ctx)
            RexConverter.Constants.cast -> visitRexCallCast(node, ctx)
            RexConverter.Constants.outerBagExcept,
            RexConverter.Constants.outerBagIntersect,
            RexConverter.Constants.outerBagUnion,
            RexConverter.Constants.outerSetExcept,
            RexConverter.Constants.outerSetIntersect,
            RexConverter.Constants.outerSetUnion -> TODO("Bag Operators have not been implemented yet.")
            else -> null
        }
    }

    private fun processRexCall(node: Rex.Call, ctx: Context): Rex.Call {
        val args = node.args.visit(ctx)
        return node.copy(args = args)
    }

    /**
     * [node] must be pre-processed
     */
    private fun visitRexCallNullIf(node: Rex.Call, ctx: Context): Rex.Call {
        // check for comparability of the two arguments to `NULLIF`
        operandsAreComparable(node.args.getTypes(ctx), node.id, ctx)

        // output type will be the first argument's types along with `NULL` (even in the case of an error)
        val possibleOutputTypes = node.args[0].grabType()?.asNullable() ?: handleMissingType(ctx)
        return node.copy(type = possibleOutputTypes)
    }

    /**
     * [node] must be pre-processed
     */
    private fun visitRexCallCast(node: Rex.Call, ctx: Context): Rex.Call {
        val sourceType = node.args[0].grabType() ?: handleMissingType(ctx)
        val targetType = node.args[1].grabType() ?: handleMissingType(ctx)
        val targetTypeParam = targetType.toTypedOpParameter()
        val castOutputType = sourceType.cast(targetType).let {
            if (targetTypeParam.validationThunk == null) {
                // There is no additional validation for this parameter, return this type as-is
                it
            } else {
                StaticType.unionOf(StaticType.MISSING, it)
            }
        }
        return node.copy(type = castOutputType)
    }

    private fun StaticType.toTypedOpParameter(): TypedOpParameter {
        return TypedOpParameter(staticType = this)
    }

    /**
     * [node] must be pre-processed
     */
    private fun visitRexCallCoalesce(node: Rex.Call, ctx: Context): Rex.Call {
        var allMissing = true
        val outputTypes = mutableSetOf<StaticType>()

        val args = node.args.map { visitArg(it, ctx) }
        for (arg in args) {
            val staticType = arg.grabType() ?: handleMissingType(ctx)
            val staticTypes = staticType.allTypes
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

        return node.copy(
            type = when (outputTypes.size) {
                1 -> outputTypes.first()
                else -> StaticType.unionOf(outputTypes)
            }
        )
    }

    /**
     * [node] must be pre-processed
     */
    private fun visitRexCallLike(node: Rex.Call, ctx: Context): Rex.Call {
        val argTypes = node.args.getTypes(ctx)
        val argsAllTypes = argTypes.map { it.allTypes }

        if (!hasValidOperandTypes(argTypes, "LIKE", ctx) { it.isText() }) {
            return node.copy(type = StaticType.BOOL)
        }

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
                    if (node.args.getOrNull(2) != null) {
                        possibleReturnTypes.add(StaticType.MISSING)
                    }
                }
                else -> possibleReturnTypes.add(StaticType.MISSING)
            }
        }

        return node.copy(type = StaticType.unionOf(possibleReturnTypes).flatten())
    }

    /**
     * [node] must be pre-processed
     */
    private fun visitRexCallInCollection(node: Rex.Call, ctx: Context): Rex.Call {
        val operands = node.args.getTypes(ctx)
        val lhs = operands[0]
        val rhs = operands[1]
        var errorAdded = false

        // check if any operands are unknown, then null or missing error
        if (operands.any { operand -> operand.isUnknown() }) {
            handleExpressionAlwaysReturnsNullOrMissingError(ctx)
            errorAdded = true
        }

        // if none of the [rhs] types are [CollectionType]s with comparable element types to [lhs], then data type
        // mismatch error
        if (!rhs.isUnknown() && rhs.allTypes.none {
            it is CollectionType && StaticTypeUtils.areStaticTypesComparable(it.elementType, lhs)
        }
        ) {
            handleIncompatibleDataTypesForOpError(ctx, operands, "IN")
            errorAdded = true
        }

        return when (errorAdded) {
            true -> StaticType.BOOL
            false -> computeReturnTypeForNAryIn(operands)
        }.let { node.copy(type = it) }
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
     * [node] must be pre-processed
     */
    private fun visitRexCallBetween(node: Rex.Call, ctx: Context): Rex.Call {
        val argTypes = listOf(node.args[0], node.args[1], node.args[2]).getTypes(ctx)
        if (!operandsAreComparable(argTypes, node.id, ctx)) {
            return node.copy(type = StaticType.BOOL)
        }

        val argsAllTypes = argTypes.map { it.allTypes }
        val possibleReturnTypes: MutableSet<SingleType> = mutableSetOf()

        argsAllTypes.cartesianProduct().forEach { argsChildType ->
            val argsSingleType = argsChildType.map { it as SingleType }
            when {
                // If any one of the operands is null or missing, return NULL
                argsSingleType.any { it is NullType || it is MissingType } -> possibleReturnTypes.add(StaticType.NULL)
                StaticTypeUtils.areStaticTypesComparable(
                    argsSingleType[0],
                    argsSingleType[1]
                ) || StaticTypeUtils.areStaticTypesComparable(argsSingleType[0], argsSingleType[2]) -> possibleReturnTypes.add(StaticType.BOOL)
                else -> possibleReturnTypes.add(StaticType.MISSING)
            }
        }
        return node.copy(type = StaticType.unionOf(possibleReturnTypes).flatten())
    }

    private fun List<Arg>.getTypes(ctx: Context): List<StaticType> = this.map { it.grabType() ?: handleMissingType(ctx) }

    private fun List<Arg>.visit(ctx: Context): List<Arg> = this.map { arg ->
        when (arg) {
            is Arg.Value -> {
                val rex = visitRex(arg.value, ctx) as Rex
                arg.copy(value = rex)
            }
            is Arg.Type -> arg
        }
    }

    /**
     * Verifies the given [actual] has type [expected]. If [actual] is unknown, a null or missing
     * error is given. If [actual] could never be [expected], an incompatible data types for
     * expression error is given.
     */
    private fun assertType(expected: StaticType, actual: StaticType, ctx: Context) {
        // Relates to `verifyExpressionType`
        if (actual.isUnknown()) {
            handleExpressionAlwaysReturnsNullOrMissingError(ctx)
        } else if (actual.allTypes.none { it == expected }) {
            handleIncompatibleDataTypeForExprError(
                expectedType = expected,
                actualType = actual,
                ctx = ctx
            )
        }
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

    private fun Rel.getTypeEnv() = PlanUtils.getTypeEnv(this)

    private fun Rel.getProperties() = this.getCommon().properties

    private fun Rel.getCommon() = when (this) {
        is Rel.Aggregate -> this.common
        is Rel.Bag -> this.common
        is Rel.Fetch -> this.common
        is Rel.Filter -> this.common
        is Rel.Join -> this.common
        is Rel.Project -> this.common
        is Rel.Scan -> this.common
        is Rel.Sort -> this.common
        is Rel.Unpivot -> this.common
    }

    private fun inferPathComponentExprType(
        previousComponentType: StaticType,
        currentPathComponent: Step.Key,
        ctx: Context
    ): StaticType =
        when (previousComponentType) {
            is AnyType -> StaticType.ANY
            is StructType -> inferStructLookupType(
                currentPathComponent,
                previousComponentType
            ).flatten()
            is ListType,
            is SexpType -> {
                val previous = previousComponentType as CollectionType // help Kotlin's type inference to be more specific
                val key = visitRex(currentPathComponent.value, ctx = ctx)
                if (key.grabType() is IntType) {
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
                            val staticTypes = prevTypes.map { inferPathComponentExprType(it, currentPathComponent, ctx) }
                            AnyOfType(staticTypes.toSet()).flatten()
                        }
                    }
                }
            }
            else -> StaticType.MISSING
        }

    private fun inferStructLookupType(
        currentPathComponent: Step.Key,
        struct: StructType
    ): StaticType =
        when (val key = currentPathComponent.value) {
            is Rex.Lit -> {
                if (key.value is StringElement) {
                    val case = rexCaseToBindingCase(currentPathComponent.case)
                    ReferenceResolver.inferStructLookup(struct, BindingName(key.value.asAnyElement().stringValue, case))
                        ?: when (struct.contentClosed) {
                            true -> StaticType.MISSING
                            false -> StaticType.ANY
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

    private fun rexBindingNameToLangBindingName(name: BindingName) = org.partiql.lang.eval.BindingName(
        name.name,
        when (name.bindingCase) {
            BindingCase.SENSITIVE -> org.partiql.lang.eval.BindingCase.SENSITIVE
            BindingCase.INSENSITIVE -> org.partiql.lang.eval.BindingCase.INSENSITIVE
        }
    )

    private fun rexIdToBindingName(node: Rex.Id): BindingName = BindingName(
        node.name,
        rexCaseToBindingCase(node.case)
    )

    private fun List<Binding>.toAttributes(ctx: Context) = this.map { Plan.attribute(it.name, it.grabType() ?: handleMissingType(ctx)) }

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

    private fun inferUnaryArithmeticOp(type: SingleType): SingleType = when (type) {
        // Propagate NULL or MISSING
        is NullType -> StaticType.NULL
        is MissingType -> StaticType.MISSING
        is DecimalType, is IntType, is FloatType -> type
        else -> StaticType.MISSING
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

    private fun inferNaryLogicalOp(argsStaticType: List<StaticType>, op: String, ctx: Context): StaticType {
        return when (hasValidOperandTypes(argsStaticType, op, ctx) { it is BoolType }) {
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
        }
    }

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

    private fun computeReturnTypeForNAry(
        argsStaticType: List<StaticType>,
        binaryOpInferencer: (SingleType, SingleType) -> SingleType
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

    /**
     * Computes the return type of the function call based on the [FunctionSignature.unknownArguments]
     */
    private fun computeReturnTypeForFunctionCall(signature: FunctionSignature, arguments: List<StaticType>, ctx: Context): StaticType {
        // Check for all the possible invalid number of argument cases. Throws an error if invalid number of arguments found.
        if (!signature.arity.contains(arguments.size)) {
            handleIncorrectNumberOfArgumentsToFunctionCallError(signature.name, signature.arity, arguments.size, ctx)
        }

        return when (signature.unknownArguments) {
            UnknownArguments.PROPAGATE -> returnTypeForPropagatingFunction(signature, arguments, ctx)
            UnknownArguments.PASS_THRU -> returnTypeForPassThruFunction(signature, arguments)
        }
    }

    /**
     * Computes return type for functions with [FunctionSignature.unknownArguments] as [UnknownArguments.PROPAGATE]
     */
    private fun returnTypeForPropagatingFunction(signature: FunctionSignature, arguments: List<StaticType>, ctx: Context): StaticType {
        val requiredArgs = arguments.zip(signature.requiredParameters)
        val restOfArgs = arguments.drop(signature.requiredParameters.size)

        // all args including optional/variadic
        val allArgs = requiredArgs.let { reqArgs ->
            if (restOfArgs.isNotEmpty()) {
                when {
                    signature.optionalParameter != null -> reqArgs + listOf(
                        Pair(
                            restOfArgs.first(),
                            signature.optionalParameter
                        )
                    )
                    signature.variadicParameter != null -> reqArgs + restOfArgs.map {
                        Pair(it, signature.variadicParameter.type)
                    }
                    else -> reqArgs
                }
            } else {
                reqArgs
            }
        }

        return if (functionHasValidArgTypes(signature.name, allArgs, ctx)) {
            val finalReturnTypes = signature.returnType.allTypes + allArgs.flatMap { (actualType, expectedType) ->
                listOfNotNull(
                    // if any type is `MISSING`, add `MISSING` to possible return types.
                    // if the actual type is not a subtype is the expected type, add `MISSING`. In the future, may
                    // want to give a warning that a data type mismatch could occur
                    // (https://github.com/partiql/partiql-lang-kotlin/issues/507)
                    StaticType.MISSING.takeIf {
                        actualType.allTypes.any { it is MissingType } || !StaticTypeUtils.isSubTypeOf(
                            actualType.filterNullMissing(),
                            expectedType
                        )
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
     * For [this] [StaticType], filters out [NullType] and [MissingType] from [AnyOfType]s. Otherwise, returns [this].
     */
    private fun StaticType.filterNullMissing(): StaticType =
        when (this) {
            is AnyOfType -> AnyOfType(this.types.filter { !it.isNullOrMissing() }.toSet()).flatten()
            else -> this
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

    /**
     * Returns true if for every pair (expr, expectedType) in [argsWithExpectedTypes], the expr's [StaticType] is
     * not an unknown and has a shared type with expectedType. Returns false otherwise.
     *
     * If an argument has an unknown type, the [SemanticProblemDetails.NullOrMissingFunctionArgument] error is
     * handled by [ProblemHandler]. If an expr has no shared type with the expectedType, the
     * [SemanticProblemDetails.InvalidArgumentTypeForFunction] error is handled by [ProblemHandler].
     */
    private fun functionHasValidArgTypes(functionName: String, argsWithExpectedTypes: List<Pair<StaticType, StaticType>>, ctx: Context): Boolean {
        var allArgsValid = true
        argsWithExpectedTypes.forEach { (actualType, expectedType) ->
            if (actualType.isUnknown()) {
                handleNullOrMissingFunctionArgument(functionName, ctx)
                allArgsValid = false
            } else {
                val actualNonUnknownType = actualType.filterNullMissing()
                if (StaticTypeUtils.getTypeDomain(actualNonUnknownType).intersect(StaticTypeUtils.getTypeDomain(expectedType)).isEmpty()
                ) {
                    handleInvalidArgumentTypeForFunction(
                        functionName = functionName,
                        expectedType = expectedType,
                        actualType = actualType,
                        ctx
                    )
                    allArgsValid = false
                }
            }
        }
        return allArgsValid
    }

    /**
     * Computes return type for functions with [FunctionSignature.unknownArguments] as [UnknownArguments.PASS_THRU]
     */
    private fun returnTypeForPassThruFunction(signature: FunctionSignature, arguments: List<StaticType>): StaticType {
        return when {
            matchesAllArguments(arguments, signature) -> signature.returnType
            matchesAtLeastOneArgument(arguments, signature) -> StaticType.unionOf(signature.returnType, StaticType.MISSING)
            else -> StaticType.MISSING
        }
    }

    /**
     * Function assumes the number of [arguments] passed agrees with the [signature]
     * Returns true when all the arguments (required, optional, variadic) are subtypes of the expected arguments for the [signature].
     * Returns false otherwise
     */
    private fun matchesAllArguments(arguments: List<StaticType>, signature: FunctionSignature): Boolean {
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
            return StaticTypeUtils.isSubTypeOf(lhs, expected)
        }

        val requiredArgumentsMatch = arguments
            .zip(signature.requiredParameters)
            .all { (actual, expected) ->
                isSubType(actual, expected)
            }

        val optionalArgumentMatches = when (signature.optionalParameter) {
            null -> true
            else -> {
                val st = arguments
                    .getOrNull(signature.requiredParameters.size)
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
                        val st = arg
                        isSubType(st, signature.variadicParameter.type)
                    }
        }

        return requiredArgumentsMatch && optionalArgumentMatches && variadicArgumentsMatch
    }

    private fun Rex.isProjectAll(): Boolean {
        return when (this) {
            is Rex.Path -> {
                val step = this.steps.lastOrNull() ?: return false
                step is Step.Wildcard
            }
            else -> false
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
    private fun matchesAtLeastOneArgument(arguments: List<StaticType>, signature: FunctionSignature): Boolean {
        val requiredArgumentsMatch = arguments
            .zip(signature.requiredParameters)
            .all { (actual, expected) ->
                StaticTypeUtils.getTypeDomain(actual).intersect(StaticTypeUtils.getTypeDomain(expected)).isNotEmpty()
            }

        val optionalArgumentMatches = when (signature.optionalParameter) {
            null -> true
            else ->
                arguments
                    .getOrNull(signature.requiredParameters.size)
                    ?.let { StaticTypeUtils.getTypeDomain(it) }
                    ?.intersect(StaticTypeUtils.getTypeDomain(signature.optionalParameter))
                    ?.isNotEmpty()
                    ?: true
        }

        val variadicArgumentsMatch = when (signature.variadicParameter) {
            null -> true
            else ->
                arguments
                    .drop(signature.requiredParameters.size)
                    .all { arg ->
                        val argType = arg
                        StaticTypeUtils.getTypeDomain(argType)
                            .intersect(StaticTypeUtils.getTypeDomain(signature.variadicParameter.type)).isNotEmpty()
                    }
        }

        return requiredArgumentsMatch && optionalArgumentMatches && variadicArgumentsMatch
    }

    private fun inferEqNeOp(lhs: SingleType, rhs: SingleType): SingleType = when {
        // Propagate missing as missing. Missing has precedence over null
        lhs is MissingType || rhs is MissingType -> StaticType.MISSING
        lhs.isNullable() || rhs.isNullable() -> StaticType.NULL
        else -> StaticType.BOOL
    }

    // LT, LTE, GT, GTE
    private fun inferComparatorOp(lhs: SingleType, rhs: SingleType): SingleType = when {
        // Propagate missing as missing. Missing has precedence over null
        lhs is MissingType || rhs is MissingType -> StaticType.MISSING
        lhs is NullType || rhs is NullType -> StaticType.NULL
        StaticTypeUtils.areStaticTypesComparable(lhs, rhs) -> StaticType.BOOL
        else -> StaticType.MISSING
    }

    /**
     * Returns true if all of the provided [argsStaticType] are comparable to each other and are not unknown. Otherwise,
     * returns false.
     *
     * If an operand is not comparable to another, the [SemanticProblemDetails.IncompatibleDatatypesForOp] error is
     * handled by [ProblemHandler]. If an operand is unknown, the
     * [SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing] error is handled by [ProblemHandler].
     *
     * TODO: consider if collection comparison semantics should be different (e.g. errors over warnings,
     *  more details in error message): https://github.com/partiql/partiql-lang-kotlin/issues/505
     */
    private fun operandsAreComparable(argsStaticType: List<StaticType>, op: String, ctx: Context): Boolean {
        var hasValidOperands = true

        // check for comparability of all operands. currently only adds one data type mismatch error
        outerLoop@ for (i in argsStaticType.indices) {
            for (j in i + 1 until argsStaticType.size) {
                if (!StaticTypeUtils.areStaticTypesComparable(argsStaticType[i], argsStaticType[j])) {
                    handleIncompatibleDataTypesForOpError(ctx, argsStaticType, op)
                    hasValidOperands = false
                    break@outerLoop
                }
            }
        }

        // check for an unknown operand type
        if (argsStaticType.any { operand -> operand.isUnknown() }) {
            handleExpressionAlwaysReturnsNullOrMissingError(ctx)
            hasValidOperands = false
        }
        return hasValidOperands
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

    private fun hasValidOperandTypes(
        operandsStaticType: List<StaticType>,
        op: String,
        ctx: Context,
        operandTypeValidator: (StaticType) -> Boolean
    ): Boolean {
        // check for an incompatible operand type
        if (operandsStaticType.any { operandStaticType -> !operandStaticType.isUnknown() && operandStaticType.allTypes.none(operandTypeValidator) }) {
            handleIncompatibleDataTypesForOpError(ctx, operandsStaticType, op)
        }

        // check for an unknown operand type
        if (operandsStaticType.any { operandStaticType -> operandStaticType.isUnknown() }) {
            handleExpressionAlwaysReturnsNullOrMissingError(ctx)
        }
        return true
    }

    private fun assertAsInt(type: StaticType, ctx: Context) {
        if (type.flatten().allTypes.any { variant -> variant is IntType }.not()) {
            handleIncompatibleDataTypeForExprError(StaticType.INT, type, ctx)
        }
    }

    private fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)

    internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType)

    private fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)

    internal fun StaticType.isNumeric(): Boolean = (this is IntType || this is FloatType || this is DecimalType)

    private fun rexCaseToBindingCase(node: Case): BindingCase = when (node) {
        Case.SENSITIVE -> BindingCase.SENSITIVE
        Case.INSENSITIVE -> BindingCase.INSENSITIVE
    }

    private fun findBind(path: BindingPath, qualifier: Rex.Id.Qualifier, ctx: Context): ReferenceResolver.ResolvedType {
        val scopingOrder = when (qualifier) {
            Rex.Id.Qualifier.LOCALS_FIRST -> ScopingOrder.LEXICAL_THEN_GLOBALS
            Rex.Id.Qualifier.UNQUALIFIED -> ctx.scopingOrder
        }
        return when (scopingOrder) {
            ScopingOrder.GLOBALS_THEN_LEXICAL -> ReferenceResolver.resolveGlobalBind(path, ctx)
                ?: ReferenceResolver.resolveLocalBind(path, ctx.inputTypeEnv)
                ?: handleUnresolvedDescriptor(path.steps.last(), ctx) {
                    ReferenceResolver.ResolvedType(StaticType.ANY)
                }
            ScopingOrder.LEXICAL_THEN_GLOBALS -> ReferenceResolver.resolveLocalBind(path, ctx.inputTypeEnv)
                ?: ReferenceResolver.resolveGlobalBind(path, ctx)
                ?: handleUnresolvedDescriptor(path.steps.last(), ctx) {
                    ReferenceResolver.ResolvedType(StaticType.ANY)
                }
        }
    }

    private fun <T> handleUnresolvedDescriptor(name: BindingName, ctx: Context, input: () -> T): T {
        return when (ctx.tolerance) {
            MinimumTolerance.FULL -> {
                handleUndefinedVariable(name, ctx)
                input.invoke()
            }
            MinimumTolerance.PARTIAL -> input.invoke()
        }
    }

    private fun grabFirstIds(node: Rex.Path): List<Rex.Id> {
        if (node.root !is Rex.Id) { return emptyList() }
        val steps = node.steps.map {
            when (it) {
                is Step.Key -> when (val value = it.value) {
                    is Rex.Lit -> {
                        val ionElement = value.value.asAnyElement()
                        when (ionElement.type) {
                            ElementType.SYMBOL, ElementType.STRING -> {
                                val stringValue = value.value.asAnyElement().stringValueOrNull
                                stringValue?.let { str ->
                                    Plan.rexId(str, it.case, Rex.Id.Qualifier.UNQUALIFIED, null)
                                }
                            }
                            else -> null
                        }
                    }
                    else -> null
                }
                else -> null
            }
        }
        val nullPosition = when (val nullIndex = steps.indexOf(null)) {
            -1 -> steps.size
            else -> nullIndex
        }
        val firstSteps = steps.subList(0, nullPosition).filterNotNull()
        return listOf(node.root as Rex.Id) + firstSteps
    }

    private fun inferType(expr: Rex, input: Rel?, ctx: Context): StaticType {
        return type(
            expr,
            Context(
                input,
                ctx.session,
                ctx.metadata,
                ScopingOrder.LEXICAL_THEN_GLOBALS,
                ctx.customFunctionSignatures,
                ctx.tolerance,
                ctx.problemHandler
            )
        ).grabType() ?: handleMissingType(ctx)
    }

    private fun typeRex(expr: Rex, input: Rel?, ctx: Context): Rex {
        return type(
            expr,
            Context(
                input,
                ctx.session,
                ctx.metadata,
                ctx.scopingOrder,
                ctx.customFunctionSignatures,
                ctx.tolerance,
                ctx.problemHandler
            )
        )
    }

    private fun typeRel(rel: Rel, input: Rel?, ctx: Context): Rel {
        return visitRel(
            rel,
            Context(
                input,
                ctx.session,
                ctx.metadata,
                ctx.scopingOrder,
                ctx.customFunctionSignatures,
                ctx.tolerance,
                ctx.problemHandler
            )
        )
    }

    private fun handleExpressionAlwaysReturnsNullOrMissingError(ctx: Context) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing
            )
        )
    }

    // TODO: https://github.com/partiql/partiql-lang-kotlin/issues/508 consider not working directly with strings for `op`
    private fun handleIncompatibleDataTypesForOpError(ctx: Context, actualTypes: List<StaticType>, op: String) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.IncompatibleDatatypesForOp(
                    actualTypes,
                    op
                )
            )
        )
    }

    private fun handleNoSuchFunctionError(ctx: Context, functionName: String) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.NoSuchFunction(functionName)
            )
        )
    }

    private fun handleIncompatibleDataTypeForExprError(expectedType: StaticType, actualType: StaticType, ctx: Context) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.IncompatibleDataTypeForExpr(expectedType, actualType)
            )
        )
    }

    private fun handleIncorrectNumberOfArgumentsToFunctionCallError(
        functionName: String,
        expectedArity: IntRange,
        actualArgCount: Int,
        ctx: Context
    ) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                    functionName,
                    expectedArity,
                    actualArgCount
                )
            )
        )
    }

    private fun handleNullOrMissingFunctionArgument(functionName: String, ctx: Context) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.NullOrMissingFunctionArgument(
                    functionName = functionName
                )
            )
        )
    }

    private fun handleUndefinedVariable(name: BindingName, ctx: Context) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = PlanningProblemDetails.UndefinedVariable(name.name, name.bindingCase == BindingCase.SENSITIVE)
            )
        )
    }

    private fun handleInvalidArgumentTypeForFunction(functionName: String, expectedType: StaticType, actualType: StaticType, ctx: Context) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.InvalidArgumentTypeForFunction(
                    functionName = functionName,
                    expectedType = expectedType,
                    actualType = actualType
                )
            )
        )
    }

    private fun handleMissingType(ctx: Context): StaticType {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = PlanningProblemDetails.CompileError("Unable to determine type of node.")
            )
        )
        return StaticType.ANY
    }

    private fun handleDuplicateAliasesError(ctx: Context) {
        ctx.problemHandler.handleProblem(
            Problem(
                sourceLocation = UNKNOWN_SOURCE_LOCATION,
                details = SemanticProblemDetails.DuplicateAliasesInSelectListItem
            )
        )
    }
}
