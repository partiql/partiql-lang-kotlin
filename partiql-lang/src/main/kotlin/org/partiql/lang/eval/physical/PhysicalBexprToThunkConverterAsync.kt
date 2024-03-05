package org.partiql.lang.eval.physical

import com.amazon.ionelement.api.BoolElement
import com.amazon.ionelement.api.MetaContainer
import org.partiql.annotations.ExperimentalWindowFunctions
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.ThunkAsync
import org.partiql.lang.eval.ThunkValueAsync
import org.partiql.lang.eval.physical.operators.AggregateOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.CompiledAggregateFunctionAsync
import org.partiql.lang.eval.physical.operators.CompiledGroupKeyAsync
import org.partiql.lang.eval.physical.operators.CompiledSortKey
import org.partiql.lang.eval.physical.operators.CompiledSortKeyAsync
import org.partiql.lang.eval.physical.operators.CompiledWindowFunctionAsync
import org.partiql.lang.eval.physical.operators.FilterRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.JoinRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.LetRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.LimitRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.OffsetRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.ProjectRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.RelationExpressionAsync
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactoryKey
import org.partiql.lang.eval.physical.operators.RelationalOperatorKind
import org.partiql.lang.eval.physical.operators.ScanRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.SortOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.UnpivotOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.WindowRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.valueExpressionAsync
import org.partiql.lang.eval.physical.window.createBuiltinWindowFunctionAsync
import org.partiql.lang.util.toIntExact

/** Converts instances of [PartiqlPhysical.Bexpr] to any [T]. */
interface Converter<T> {
    suspend fun convert(node: PartiqlPhysical.Bexpr): T = when (node) {
        is PartiqlPhysical.Bexpr.Project -> convertProject(node)
        is PartiqlPhysical.Bexpr.Scan -> convertScan(node)
        is PartiqlPhysical.Bexpr.Unpivot -> convertUnpivot(node)
        is PartiqlPhysical.Bexpr.Filter -> convertFilter(node)
        is PartiqlPhysical.Bexpr.Join -> convertJoin(node)
        is PartiqlPhysical.Bexpr.Sort -> convertSort(node)
        is PartiqlPhysical.Bexpr.Aggregate -> convertAggregate(node)
        is PartiqlPhysical.Bexpr.Offset -> convertOffset(node)
        is PartiqlPhysical.Bexpr.Limit -> convertLimit(node)
        is PartiqlPhysical.Bexpr.Let -> convertLet(node)
        is PartiqlPhysical.Bexpr.Window -> convertWindow(node)
    }

    suspend fun convertProject(node: PartiqlPhysical.Bexpr.Project): T
    suspend fun convertScan(node: PartiqlPhysical.Bexpr.Scan): T
    suspend fun convertUnpivot(node: PartiqlPhysical.Bexpr.Unpivot): T
    suspend fun convertFilter(node: PartiqlPhysical.Bexpr.Filter): T
    suspend fun convertJoin(node: PartiqlPhysical.Bexpr.Join): T
    suspend fun convertSort(node: PartiqlPhysical.Bexpr.Sort): T
    suspend fun convertAggregate(node: PartiqlPhysical.Bexpr.Aggregate): T
    suspend fun convertOffset(node: PartiqlPhysical.Bexpr.Offset): T
    suspend fun convertLimit(node: PartiqlPhysical.Bexpr.Limit): T
    suspend fun convertLet(node: PartiqlPhysical.Bexpr.Let): T
    suspend fun convertWindow(node: PartiqlPhysical.Bexpr.Window): T
}

/** A specialization of [Thunk] that we use for evaluation of physical plans. */
internal typealias PhysicalPlanThunkAsync = ThunkAsync<EvaluatorState>

/** A specialization of [ThunkValue] that we use for evaluation of physical plans. */
internal typealias PhysicalPlanThunkValueAsync<T> = ThunkValueAsync<EvaluatorState, T>

internal class PhysicalBexprToThunkConverterAsync(
    private val exprConverter: PhysicalPlanCompilerAsync,
    private val relationalOperatorFactory: Map<RelationalOperatorFactoryKey, RelationalOperatorFactory>
) : Converter<RelationThunkEnvAsync> {

    private fun PhysicalPlanThunkAsync.toValueExpr(sourceLocationMeta: SourceLocationMeta?) =
        valueExpressionAsync(sourceLocationMeta) { state -> this(state) }

    private suspend fun RelationExpressionAsync.toRelationThunk(metas: MetaContainer) =
        relationThunkAsync(metas) { state -> this.evaluateAsync(state) }

    private inline fun <reified T : RelationalOperatorFactory> findOperatorFactory(
        operator: RelationalOperatorKind,
        name: String
    ): T {
        val key = RelationalOperatorFactoryKey(operator, name)
        val found =
            relationalOperatorFactory[key] ?: error("Factory for operator ${key.operator} named '${key.name}' does not exist.")
        return found as? T
            ?: error(
                "Internal error: Operator factory ${key.operator} named '${key.name}' does not derive from " +
                    T::class.java + "."
            )
    }

    override suspend fun convertProject(node: PartiqlPhysical.Bexpr.Project): RelationThunkEnvAsync {
        // recurse into children
        val argExprs = node.args.map { exprConverter.convert(it).toValueExpr(it.metas.sourceLocationMeta) }

        // locate operator factory
        val factory = findOperatorFactory<ProjectRelationalOperatorFactoryAsync>(RelationalOperatorKind.PROJECT, node.i.name.text)

        // create operator implementation
        val bindingsExpr = factory.create(node.i, node.binding.toSetVariableFunc(), argExprs)

        // wrap in thunk.
        return bindingsExpr.toRelationThunk(node.metas)
    }

    override suspend fun convertAggregate(node: PartiqlPhysical.Bexpr.Aggregate): RelationThunkEnvAsync {
        val source = this.convert(node.source)

        // Compile Arguments
        val compiledFunctions = node.functionList.functions.map { func ->
            val setAggregateVal = func.asVar.toSetVariableFunc()
            val value = exprConverter.convert(func.arg).toValueExpr(func.arg.metas.sourceLocationMeta)
            CompiledAggregateFunctionAsync(func.name.text, setAggregateVal, value, func.quantifier)
        }
        val compiledKeys = node.groupList.keys.map { key ->
            val value = exprConverter.convert(key.expr).toValueExpr(key.expr.metas.sourceLocationMeta)
            val function = key.asVar.toSetVariableFunc()
            CompiledGroupKeyAsync(function, value, key.asVar)
        }

        // Get Implementation
        val factory = findOperatorFactory<AggregateOperatorFactoryAsync>(RelationalOperatorKind.AGGREGATE, node.i.name.text)
        val relationExpression = factory.create({ state -> source.invoke(state) }, node.strategy, compiledKeys, compiledFunctions)
        return relationExpression.toRelationThunk(node.metas)
    }

    override suspend fun convertScan(node: PartiqlPhysical.Bexpr.Scan): RelationThunkEnvAsync {
        // recurse into children
        val valueExpr = exprConverter.convert(node.expr).toValueExpr(node.expr.metas.sourceLocationMeta)
        val asSetter = node.asDecl.toSetVariableFunc()
        val atSetter = node.atDecl?.toSetVariableFunc()
        val bySetter = node.byDecl?.toSetVariableFunc()

        // locate operator factory
        val factory = findOperatorFactory<ScanRelationalOperatorFactoryAsync>(RelationalOperatorKind.SCAN, node.i.name.text)

        // create operator implementation
        val bindingsExpr = factory.create(
            impl = node.i,
            expr = valueExpr,
            setAsVar = asSetter,
            setAtVar = atSetter,
            setByVar = bySetter
        )

        // wrap in thunk
        return bindingsExpr.toRelationThunk(node.metas)
    }

    override suspend fun convertUnpivot(node: PartiqlPhysical.Bexpr.Unpivot): RelationThunkEnvAsync {
        val valueExpr = exprConverter.convert(node.expr).toValueExpr(node.expr.metas.sourceLocationMeta)
        val asSetter = node.asDecl.toSetVariableFunc()
        val atSetter = node.atDecl?.toSetVariableFunc()
        val bySetter = node.byDecl?.toSetVariableFunc()

        val factory = findOperatorFactory<UnpivotOperatorFactoryAsync>(RelationalOperatorKind.UNPIVOT, node.i.name.text)

        val bindingsExpr = factory.create(
            expr = valueExpr,
            setAsVar = asSetter,
            setAtVar = atSetter,
            setByVar = bySetter
        )

        return bindingsExpr.toRelationThunk(node.metas)
    }

    override suspend fun convertFilter(node: PartiqlPhysical.Bexpr.Filter): RelationThunkEnvAsync {
        // recurse into children
        val predicateValueExpr = exprConverter.convert(node.predicate).toValueExpr(node.predicate.metas.sourceLocationMeta)
        val sourceBindingsExpr = this.convert(node.source)

        // locate operator factory
        val factory = findOperatorFactory<FilterRelationalOperatorFactoryAsync>(RelationalOperatorKind.FILTER, node.i.name.text)

        // create operator implementation
        val bindingsExpr = factory.create(node.i, predicateValueExpr) { state -> sourceBindingsExpr.invoke(state) }

        // wrap in thunk
        return bindingsExpr.toRelationThunk(node.metas)
    }

    override suspend fun convertJoin(node: PartiqlPhysical.Bexpr.Join): RelationThunkEnvAsync {
        // recurse into children
        val leftBindingsExpr = this.convert(node.left)
        val rightBindingdExpr = this.convert(node.right)
        val predicateValueExpr = node.predicate?.let { predicate ->
            exprConverter.convert(predicate)
                .takeIf { !predicate.isLitTrue() }
                ?.toValueExpr(predicate.metas.sourceLocationMeta)
        }

        // locate operator factory
        val factory = findOperatorFactory<JoinRelationalOperatorFactoryAsync>(RelationalOperatorKind.JOIN, node.i.name.text)

        // Compute a function to set the left-side variables to NULL.  This is for use with RIGHT JOIN, when the left
        // side of the join is empty or no rows match the predicate.
        val leftVariableIndexes = node.left.extractAccessibleVarDecls().map { it.index.value.toIntExact() }
        val setLeftSideVariablesToNull: (EvaluatorState) -> Unit = { state ->
            leftVariableIndexes.forEach { state.registers[it] = ExprValue.nullValue }
        }
        // Compute a function to set the right-side variables to NULL.  This is for use with LEFT JOIN, when the right
        // side of the join is empty or no rows match the predicate.
        val rightVariableIndexes = node.right.extractAccessibleVarDecls().map { it.index.value.toIntExact() }
        val setRightSideVariablesToNull: (EvaluatorState) -> Unit = { state ->
            rightVariableIndexes.forEach { state.registers[it] = ExprValue.nullValue }
        }

        return factory.create(
            impl = node.i,
            joinType = node.joinType,
            leftBexpr = { state -> leftBindingsExpr(state) },
            rightBexpr = { state -> rightBindingdExpr(state) },
            predicateExpr = predicateValueExpr,
            setLeftSideVariablesToNull = setLeftSideVariablesToNull,
            setRightSideVariablesToNull = setRightSideVariablesToNull
        ).toRelationThunk(node.metas)
    }

    private fun PartiqlPhysical.Bexpr.extractAccessibleVarDecls(): List<PartiqlPhysical.VarDecl> =
        // This fold traverses a [PartiqlPhysical.Bexpr] node and extracts all variable declarations within
        // It avoids recursing into sub-queries.
        object : PartiqlPhysical.VisitorFold<List<PartiqlPhysical.VarDecl>>() {
            override fun visitVarDecl(
                node: PartiqlPhysical.VarDecl,
                accumulator: List<PartiqlPhysical.VarDecl>
            ): List<PartiqlPhysical.VarDecl> = accumulator + node

            /**
             * Avoids recursion into expressions, since these may contain sub-queries with other var-decls that we don't
             * care about here.
             */
            override fun walkExpr(
                node: PartiqlPhysical.Expr,
                accumulator: List<PartiqlPhysical.VarDecl>
            ): List<PartiqlPhysical.VarDecl> {
                return accumulator
            }
        }.walkBexpr(this, emptyList())

    override suspend fun convertOffset(node: PartiqlPhysical.Bexpr.Offset): RelationThunkEnvAsync {
        // recurse into children
        val rowCountExpr = exprConverter.convert(node.rowCount).toValueExpr(node.rowCount.metas.sourceLocationMeta)
        val sourceBexpr = this.convert(node.source)

        // locate operator factory
        val factory = findOperatorFactory<OffsetRelationalOperatorFactoryAsync>(RelationalOperatorKind.OFFSET, node.i.name.text)

        // create operator implementation
        val bindingsExpr = factory.create(node.i, rowCountExpr) { state -> sourceBexpr(state) }
        // wrap in thunk
        return bindingsExpr.toRelationThunk(node.metas)
    }

    override suspend fun convertLimit(node: PartiqlPhysical.Bexpr.Limit): RelationThunkEnvAsync {
        // recurse into children
        val rowCountExpr = exprConverter.convert(node.rowCount).toValueExpr(node.rowCount.metas.sourceLocationMeta)
        val sourceBexpr = this.convert(node.source)

        // locate operator factory
        val factory = findOperatorFactory<LimitRelationalOperatorFactoryAsync>(RelationalOperatorKind.LIMIT, node.i.name.text)

        // create operator implementation
        val bindingsExpr = factory.create(node.i, rowCountExpr) { state -> sourceBexpr(state) }

        // wrap in thunk
        return bindingsExpr.toRelationThunk(node.metas)
    }

    override suspend fun convertSort(node: PartiqlPhysical.Bexpr.Sort): RelationThunkEnvAsync {
        // Compile Arguments
        val source = this.convert(node.source)
        val sortKeys = compileSortSpecsAsync(node.sortSpecs)

        // Get Implementation
        val factory = findOperatorFactory<SortOperatorFactoryAsync>(RelationalOperatorKind.SORT, node.i.name.text)
        val bindingsExpr = factory.create(sortKeys, { state -> source(state) })
        return bindingsExpr.toRelationThunk(node.metas)
    }

    override suspend fun convertLet(node: PartiqlPhysical.Bexpr.Let): RelationThunkEnvAsync {
        // recurse into children
        val sourceBexpr = this.convert(node.source)
        val compiledBindings = node.bindings.map {
            VariableBindingAsync(
                it.decl.toSetVariableFunc(),
                exprConverter.convert(it.value).toValueExpr(it.value.metas.sourceLocationMeta)
            )
        }
        // locate operator factory
        val factory = findOperatorFactory<LetRelationalOperatorFactoryAsync>(RelationalOperatorKind.LET, node.i.name.text)

        // create operator implementation
        val bindingsExpr = factory.create(node.i, { state -> sourceBexpr(state) }, compiledBindings)

        // wrap in thunk
        return bindingsExpr.toRelationThunk(node.metas)
    }

    /**
     * Returns a list of [CompiledSortKey] with the aim of pre-computing the [NaturalExprValueComparators] prior to
     * evaluation and leaving the [PartiqlPhysical.SortSpec]'s [PartiqlPhysical.Expr] to be evaluated later.
     */
    private suspend fun compileSortSpecsAsync(specs: List<PartiqlPhysical.SortSpec>): List<CompiledSortKeyAsync> = specs.map { spec ->
        val comp = when (spec.orderingSpec ?: PartiqlPhysical.OrderingSpec.Asc()) {
            is PartiqlPhysical.OrderingSpec.Asc ->
                when (spec.nullsSpec) {
                    is PartiqlPhysical.NullsSpec.NullsFirst -> NaturalExprValueComparators.NULLS_FIRST_ASC
                    is PartiqlPhysical.NullsSpec.NullsLast -> NaturalExprValueComparators.NULLS_LAST_ASC
                    null -> NaturalExprValueComparators.NULLS_LAST_ASC
                }

            is PartiqlPhysical.OrderingSpec.Desc ->
                when (spec.nullsSpec) {
                    is PartiqlPhysical.NullsSpec.NullsFirst -> NaturalExprValueComparators.NULLS_FIRST_DESC
                    is PartiqlPhysical.NullsSpec.NullsLast -> NaturalExprValueComparators.NULLS_LAST_DESC
                    null -> NaturalExprValueComparators.NULLS_FIRST_DESC
                }
        }
        val value = exprConverter.convert(spec.expr).toValueExpr(spec.expr.metas.sourceLocationMeta)
        CompiledSortKeyAsync(comp, value)
    }

    @OptIn(ExperimentalWindowFunctions::class)
    override suspend fun convertWindow(node: PartiqlPhysical.Bexpr.Window): RelationThunkEnvAsync {
        val source = this.convert(node.source)

        val windowPartitionList = node.windowSpecification.partitionBy

        val windowSortSpecList = node.windowSpecification.orderBy

        val compiledPartitionBy = windowPartitionList?.exprs?.map {
            exprConverter.convert(it).toValueExpr(it.metas.sourceLocationMeta)
        } ?: emptyList()

        val compiledOrderBy = windowSortSpecList?.sortSpecs?.let { compileSortSpecsAsync(it) } ?: emptyList()

        val compiledWindowFunctions = node.windowExpressionList.map { windowExpression ->
            CompiledWindowFunctionAsync(
                createBuiltinWindowFunctionAsync(windowExpression.funcName.text),
                windowExpression.args.map { exprConverter.convert(it).toValueExpr(it.metas.sourceLocationMeta) },
                windowExpression.decl
            )
        }

        // locate operator factory
        val factory = findOperatorFactory<WindowRelationalOperatorFactoryAsync>(RelationalOperatorKind.WINDOW, node.i.name.text)

        // create operator implementation
        val bindingsExpr = factory.create({ state -> source(state) }, compiledPartitionBy, compiledOrderBy, compiledWindowFunctions)
        // wrap in thunk
        return bindingsExpr.toRelationThunk(node.metas)
    }
}

internal fun PartiqlPhysical.Expr.isLitTrue() =
    this is PartiqlPhysical.Expr.Lit && this.value is BoolElement && this.value.booleanValue
