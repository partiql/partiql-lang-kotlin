@file:Suppress("UNUSED_PARAMETER") @file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.ir.builder

import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PartiQLPlan
import org.partiql.planner.internal.ir.PartiQLVersion
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal fun <T : PlanNode> plan(block: PlanBuilder.() -> T) = PlanBuilder().block()

internal class PlanBuilder {
    internal fun partiQLPlan(
        version: PartiQLVersion? = null,
        catalogs: MutableList<Catalog> = mutableListOf(),
        statement: Statement? = null,
        block: PartiQlPlanBuilder.() -> Unit = {},
    ): PartiQLPlan {
        val builder = PartiQlPlanBuilder(version, catalogs, statement)
        builder.block()
        return builder.build()
    }

    internal fun catalog(
        name: String? = null,
        symbols: MutableList<Catalog.Symbol> = mutableListOf(),
        block: CatalogBuilder.() -> Unit = {},
    ): Catalog {
        val builder = CatalogBuilder(name, symbols)
        builder.block()
        return builder.build()
    }

    internal fun catalogSymbol(
        path: MutableList<String> = mutableListOf(),
        type: StaticType? = null,
        block: CatalogSymbolBuilder.() -> Unit = {},
    ): Catalog.Symbol {
        val builder = CatalogSymbolBuilder(path, type)
        builder.block()
        return builder.build()
    }

    internal fun catalogSymbolRef(
        catalog: Int? = null,
        symbol: Int? = null,
        block: CatalogSymbolRefBuilder.() -> Unit = {},
    ): Catalog.Symbol.Ref {
        val builder = CatalogSymbolRefBuilder(catalog, symbol)
        builder.block()
        return builder.build()
    }

    internal fun fnResolved(
        signature: FunctionSignature.Scalar? = null,
        block: FnResolvedBuilder.() -> Unit = {},
    ): Fn.Resolved {
        val builder = FnResolvedBuilder(signature)
        builder.block()
        return builder.build()
    }

    internal fun fnUnresolved(
        identifier: Identifier? = null,
        isHidden: Boolean? = null,
        block: FnUnresolvedBuilder.() -> Unit = {},
    ): Fn.Unresolved {
        val builder = FnUnresolvedBuilder(identifier, isHidden)
        builder.block()
        return builder.build()
    }

    internal fun aggResolved(
        signature: FunctionSignature.Aggregation? = null,
        block: AggResolvedBuilder.() -> Unit = {},
    ): Agg.Resolved {
        val builder = AggResolvedBuilder(signature)
        builder.block()
        return builder.build()
    }

    internal fun aggUnresolved(
        identifier: Identifier? = null,
        block: AggUnresolvedBuilder.() -> Unit = {},
    ): Agg.Unresolved {
        val builder = AggUnresolvedBuilder(identifier)
        builder.block()
        return builder.build()
    }

    internal fun statementQuery(root: Rex? = null, block: StatementQueryBuilder.() -> Unit = {}): Statement.Query {
        val builder = StatementQueryBuilder(root)
        builder.block()
        return builder.build()
    }

    internal fun identifierSymbol(
        symbol: String? = null,
        caseSensitivity: Identifier.CaseSensitivity? = null,
        block: IdentifierSymbolBuilder.() -> Unit = {},
    ): Identifier.Symbol {
        val builder = IdentifierSymbolBuilder(symbol, caseSensitivity)
        builder.block()
        return builder.build()
    }

    internal fun identifierQualified(
        root: Identifier.Symbol? = null,
        steps: MutableList<Identifier.Symbol> = mutableListOf(),
        block: IdentifierQualifiedBuilder.() -> Unit = {},
    ): Identifier.Qualified {
        val builder = IdentifierQualifiedBuilder(root, steps)
        builder.block()
        return builder.build()
    }

    internal fun rex(
        type: StaticType? = null,
        op: Rex.Op? = null,
        block: RexBuilder.() -> Unit = {},
    ): Rex {
        val builder = RexBuilder(type, op)
        builder.block()
        return builder.build()
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun rexOpLit(`value`: PartiQLValue? = null, block: RexOpLitBuilder.() -> Unit = {}): Rex.Op.Lit {
        val builder = RexOpLitBuilder(value)
        builder.block()
        return builder.build()
    }

    internal fun rexOpVarResolved(
        ref: Int? = null,
        block: RexOpVarResolvedBuilder.() -> Unit = {},
    ): Rex.Op.Var.Resolved {
        val builder = RexOpVarResolvedBuilder(ref)
        builder.block()
        return builder.build()
    }

    internal fun rexOpVarUnresolved(
        identifier: Identifier? = null,
        scope: Rex.Op.Var.Scope? = null,
        block: RexOpVarUnresolvedBuilder.() -> Unit = {},
    ): Rex.Op.Var.Unresolved {
        val builder = RexOpVarUnresolvedBuilder(identifier, scope)
        builder.block()
        return builder.build()
    }

    internal fun rexOpGlobal(
        ref: Catalog.Symbol.Ref? = null,
        block: RexOpGlobalBuilder.() -> Unit = {}
    ): Rex.Op.Global {
        val builder = RexOpGlobalBuilder(ref)
        builder.block()
        return builder.build()
    }

    internal fun rexOpPathIndex(
        root: Rex? = null,
        key: Rex? = null,
        block: RexOpPathIndexBuilder.() -> Unit = {},
    ): Rex.Op.Path.Index {
        val builder = RexOpPathIndexBuilder(root, key)
        builder.block()
        return builder.build()
    }

    internal fun rexOpPathKey(
        root: Rex? = null,
        key: Rex? = null,
        block: RexOpPathKeyBuilder.() -> Unit = {},
    ): Rex.Op.Path.Key {
        val builder = RexOpPathKeyBuilder(root, key)
        builder.block()
        return builder.build()
    }

    internal fun rexOpPathSymbol(
        root: Rex? = null,
        key: String? = null,
        block: RexOpPathSymbolBuilder.() -> Unit = {},
    ): Rex.Op.Path.Symbol {
        val builder = RexOpPathSymbolBuilder(root, key)
        builder.block()
        return builder.build()
    }

    internal fun rexOpCallStatic(
        fn: Fn? = null,
        args: MutableList<Rex> = mutableListOf(),
        block: RexOpCallStaticBuilder.() -> Unit = {},
    ): Rex.Op.Call.Static {
        val builder = RexOpCallStaticBuilder(fn, args)
        builder.block()
        return builder.build()
    }

    internal fun rexOpCallDynamic(
        args: MutableList<Rex> = mutableListOf(),
        candidates: MutableList<Rex.Op.Call.Dynamic.Candidate> = mutableListOf(),
        block: RexOpCallDynamicBuilder.() -> Unit = {},
    ): Rex.Op.Call.Dynamic {
        val builder = RexOpCallDynamicBuilder(args, candidates)
        builder.block()
        return builder.build()
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun rexOpCallDynamicCandidate(
        fn: Fn? = null,
        parameters: MutableList<PartiQLValueType> = mutableListOf(),
        coercions: MutableList<Fn?> = mutableListOf(),
        block: RexOpCallDynamicCandidateBuilder.() -> Unit = {},
    ): Rex.Op.Call.Dynamic.Candidate {
        val builder = RexOpCallDynamicCandidateBuilder(fn, parameters, coercions)
        builder.block()
        return builder.build()
    }

    internal fun rexOpCase(
        branches: MutableList<Rex.Op.Case.Branch> = mutableListOf(),
        default: Rex? = null,
        block: RexOpCaseBuilder.() -> Unit = {},
    ): Rex.Op.Case {
        val builder = RexOpCaseBuilder(branches, default)
        builder.block()
        return builder.build()
    }

    internal fun rexOpCaseBranch(
        condition: Rex? = null,
        rex: Rex? = null,
        block: RexOpCaseBranchBuilder.() -> Unit = {},
    ): Rex.Op.Case.Branch {
        val builder = RexOpCaseBranchBuilder(condition, rex)
        builder.block()
        return builder.build()
    }

    internal fun rexOpCollection(
        values: MutableList<Rex> = mutableListOf(),
        block: RexOpCollectionBuilder.() -> Unit = {},
    ): Rex.Op.Collection {
        val builder = RexOpCollectionBuilder(values)
        builder.block()
        return builder.build()
    }

    internal fun rexOpStruct(
        fields: MutableList<Rex.Op.Struct.Field> = mutableListOf(),
        block: RexOpStructBuilder.() -> Unit = {},
    ): Rex.Op.Struct {
        val builder = RexOpStructBuilder(fields)
        builder.block()
        return builder.build()
    }

    internal fun rexOpStructField(
        k: Rex? = null,
        v: Rex? = null,
        block: RexOpStructFieldBuilder.() -> Unit = {},
    ): Rex.Op.Struct.Field {
        val builder = RexOpStructFieldBuilder(k, v)
        builder.block()
        return builder.build()
    }

    internal fun rexOpPivot(
        key: Rex? = null,
        `value`: Rex? = null,
        rel: Rel? = null,
        block: RexOpPivotBuilder.() -> Unit = {},
    ): Rex.Op.Pivot {
        val builder = RexOpPivotBuilder(key, value, rel)
        builder.block()
        return builder.build()
    }

    internal fun rexOpSubquery(
        select: Rex.Op.Select? = null,
        coercion: Rex.Op.Subquery.Coercion? = null,
        block: RexOpSubqueryBuilder.() -> Unit = {},
    ): Rex.Op.Subquery {
        val builder = RexOpSubqueryBuilder(select, coercion)
        builder.block()
        return builder.build()
    }

    internal fun rexOpSelect(
        `constructor`: Rex? = null,
        rel: Rel? = null,
        block: RexOpSelectBuilder.() -> Unit = {},
    ): Rex.Op.Select {
        val builder = RexOpSelectBuilder(constructor, rel)
        builder.block()
        return builder.build()
    }

    internal fun rexOpTupleUnion(
        args: MutableList<Rex> = mutableListOf(),
        block: RexOpTupleUnionBuilder.() -> Unit = {},
    ): Rex.Op.TupleUnion {
        val builder = RexOpTupleUnionBuilder(args)
        builder.block()
        return builder.build()
    }

    internal fun rexOpErr(message: String? = null, block: RexOpErrBuilder.() -> Unit = {}): Rex.Op.Err {
        val builder = RexOpErrBuilder(message)
        builder.block()
        return builder.build()
    }

    internal fun rel(
        type: Rel.Type? = null,
        op: Rel.Op? = null,
        block: RelBuilder.() -> Unit = {},
    ): Rel {
        val builder = RelBuilder(type, op)
        builder.block()
        return builder.build()
    }

    internal fun relType(
        schema: MutableList<Rel.Binding> = mutableListOf(),
        props: MutableSet<Rel.Prop> = mutableSetOf(),
        block: RelTypeBuilder.() -> Unit = {},
    ): Rel.Type {
        val builder = RelTypeBuilder(schema, props)
        builder.block()
        return builder.build()
    }

    internal fun relOpScan(rex: Rex? = null, block: RelOpScanBuilder.() -> Unit = {}): Rel.Op.Scan {
        val builder = RelOpScanBuilder(rex)
        builder.block()
        return builder.build()
    }

    internal fun relOpScanIndexed(
        rex: Rex? = null,
        block: RelOpScanIndexedBuilder.() -> Unit = {},
    ): Rel.Op.ScanIndexed {
        val builder = RelOpScanIndexedBuilder(rex)
        builder.block()
        return builder.build()
    }

    internal fun relOpUnpivot(rex: Rex? = null, block: RelOpUnpivotBuilder.() -> Unit = {}): Rel.Op.Unpivot {
        val builder = RelOpUnpivotBuilder(rex)
        builder.block()
        return builder.build()
    }

    internal fun relOpDistinct(input: Rel? = null, block: RelOpDistinctBuilder.() -> Unit = {}): Rel.Op.Distinct {
        val builder = RelOpDistinctBuilder(input)
        builder.block()
        return builder.build()
    }

    internal fun relOpFilter(
        input: Rel? = null,
        predicate: Rex? = null,
        block: RelOpFilterBuilder.() -> Unit = {},
    ): Rel.Op.Filter {
        val builder = RelOpFilterBuilder(input, predicate)
        builder.block()
        return builder.build()
    }

    internal fun relOpSort(
        input: Rel? = null,
        specs: MutableList<Rel.Op.Sort.Spec> = mutableListOf(),
        block: RelOpSortBuilder.() -> Unit = {},
    ): Rel.Op.Sort {
        val builder = RelOpSortBuilder(input, specs)
        builder.block()
        return builder.build()
    }

    internal fun relOpSortSpec(
        rex: Rex? = null,
        order: Rel.Op.Sort.Order? = null,
        block: RelOpSortSpecBuilder.() -> Unit = {},
    ): Rel.Op.Sort.Spec {
        val builder = RelOpSortSpecBuilder(rex, order)
        builder.block()
        return builder.build()
    }

    internal fun relOpUnion(
        lhs: Rel? = null,
        rhs: Rel? = null,
        block: RelOpUnionBuilder.() -> Unit = {},
    ): Rel.Op.Union {
        val builder = RelOpUnionBuilder(lhs, rhs)
        builder.block()
        return builder.build()
    }

    internal fun relOpIntersect(
        lhs: Rel? = null,
        rhs: Rel? = null,
        block: RelOpIntersectBuilder.() -> Unit = {},
    ): Rel.Op.Intersect {
        val builder = RelOpIntersectBuilder(lhs, rhs)
        builder.block()
        return builder.build()
    }

    internal fun relOpExcept(
        lhs: Rel? = null,
        rhs: Rel? = null,
        block: RelOpExceptBuilder.() -> Unit = {},
    ): Rel.Op.Except {
        val builder = RelOpExceptBuilder(lhs, rhs)
        builder.block()
        return builder.build()
    }

    internal fun relOpLimit(
        input: Rel? = null,
        limit: Rex? = null,
        block: RelOpLimitBuilder.() -> Unit = {},
    ): Rel.Op.Limit {
        val builder = RelOpLimitBuilder(input, limit)
        builder.block()
        return builder.build()
    }

    internal fun relOpOffset(
        input: Rel? = null,
        offset: Rex? = null,
        block: RelOpOffsetBuilder.() -> Unit = {},
    ): Rel.Op.Offset {
        val builder = RelOpOffsetBuilder(input, offset)
        builder.block()
        return builder.build()
    }

    internal fun relOpProject(
        input: Rel? = null,
        projections: MutableList<Rex> = mutableListOf(),
        block: RelOpProjectBuilder.() -> Unit = {},
    ): Rel.Op.Project {
        val builder = RelOpProjectBuilder(input, projections)
        builder.block()
        return builder.build()
    }

    internal fun relOpJoin(
        lhs: Rel? = null,
        rhs: Rel? = null,
        rex: Rex? = null,
        type: Rel.Op.Join.Type? = null,
        block: RelOpJoinBuilder.() -> Unit = {},
    ): Rel.Op.Join {
        val builder = RelOpJoinBuilder(lhs, rhs, rex, type)
        builder.block()
        return builder.build()
    }

    internal fun relOpAggregate(
        input: Rel? = null,
        strategy: Rel.Op.Aggregate.Strategy? = null,
        calls: MutableList<Rel.Op.Aggregate.Call> = mutableListOf(),
        groups: MutableList<Rex> = mutableListOf(),
        block: RelOpAggregateBuilder.() -> Unit = {},
    ): Rel.Op.Aggregate {
        val builder = RelOpAggregateBuilder(input, strategy, calls, groups)
        builder.block()
        return builder.build()
    }

    internal fun relOpAggregateCall(
        agg: Agg? = null,
        args: MutableList<Rex> = mutableListOf(),
        block: RelOpAggregateCallBuilder.() -> Unit = {},
    ): Rel.Op.Aggregate.Call {
        val builder = RelOpAggregateCallBuilder(agg, args)
        builder.block()
        return builder.build()
    }

    internal fun relOpExclude(
        input: Rel? = null,
        items: MutableList<Rel.Op.Exclude.Item> = mutableListOf(),
        block: RelOpExcludeBuilder.() -> Unit = {},
    ): Rel.Op.Exclude {
        val builder = RelOpExcludeBuilder(input, items)
        builder.block()
        return builder.build()
    }

    internal fun relOpExcludeItem(
        root: Rex.Op.Var? = null,
        steps: MutableList<Rel.Op.Exclude.Step> = mutableListOf(),
        block: RelOpExcludeItemBuilder.() -> Unit = {},
    ): Rel.Op.Exclude.Item {
        val builder = RelOpExcludeItemBuilder(root, steps)
        builder.block()
        return builder.build()
    }

    internal fun relOpExcludeStepStructField(
        symbol: Identifier.Symbol? = null,
        block: RelOpExcludeStepStructFieldBuilder.() -> Unit = {}
    ): Rel.Op.Exclude.Step.StructField {
        val builder = RelOpExcludeStepStructFieldBuilder(symbol)
        builder.block()
        return builder.build()
    }

    internal fun relOpExcludeStepCollIndex(
        index: Int? = null,
        block: RelOpExcludeStepCollIndexBuilder.() -> Unit = {}
    ): Rel.Op.Exclude.Step.CollIndex {
        val builder = RelOpExcludeStepCollIndexBuilder(index)
        builder.block()
        return builder.build()
    }

    internal fun relOpExcludeStepStructWildcard(
        block: RelOpExcludeStepStructWildcardBuilder.() -> Unit =
            {}
    ): Rel.Op.Exclude.Step.StructWildcard {
        val builder = RelOpExcludeStepStructWildcardBuilder()
        builder.block()
        return builder.build()
    }

    internal fun relOpExcludeStepCollWildcard(
        block: RelOpExcludeStepCollWildcardBuilder.() -> Unit =
            {}
    ): Rel.Op.Exclude.Step.CollWildcard {
        val builder = RelOpExcludeStepCollWildcardBuilder()
        builder.block()
        return builder.build()
    }

    internal fun relOpErr(message: String? = null, block: RelOpErrBuilder.() -> Unit = {}): Rel.Op.Err {
        val builder = RelOpErrBuilder(message)
        builder.block()
        return builder.build()
    }

    internal fun relBinding(
        name: String? = null,
        type: StaticType? = null,
        block: RelBindingBuilder.() -> Unit = {},
    ): Rel.Binding {
        val builder = RelBindingBuilder(name, type)
        builder.block()
        return builder.build()
    }
}
