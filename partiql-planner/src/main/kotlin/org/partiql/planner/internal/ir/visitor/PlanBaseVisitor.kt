@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.ir.visitor

import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Global
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PartiQLPlan
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.value.PartiQLValueExperimental

internal abstract class PlanBaseVisitor<R, C> : PlanVisitor<R, C> {
    override fun visit(node: PlanNode, ctx: C): R = node.accept(this, ctx)

    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: C): R = defaultVisit(node, ctx)

    override fun visitGlobal(node: Global, ctx: C): R = defaultVisit(node, ctx)

    override fun visitFn(node: Fn, ctx: C): R = when (node) {
        is Fn.Resolved -> visitFnResolved(node, ctx)
        is Fn.Unresolved -> visitFnUnresolved(node, ctx)
        is Fn.Dynamic -> visitFnDynamic(node, ctx)
    }

    override fun visitFnResolved(node: Fn.Resolved, ctx: C): R = defaultVisit(node, ctx)

    override fun visitFnUnresolved(node: Fn.Unresolved, ctx: C): R = defaultVisit(node, ctx)

    override fun visitFnDynamic(node: Fn.Dynamic, ctx: C): R = defaultVisit(node, ctx)

    override fun visitAgg(node: Agg, ctx: C): R = when (node) {
        is Agg.Resolved -> visitAggResolved(node, ctx)
        is Agg.Unresolved -> visitAggUnresolved(node, ctx)
    }

    override fun visitAggResolved(node: Agg.Resolved, ctx: C): R = defaultVisit(node, ctx)

    override fun visitAggUnresolved(node: Agg.Unresolved, ctx: C): R = defaultVisit(node, ctx)

    override fun visitStatement(node: Statement, ctx: C): R = when (node) {
        is Statement.Query -> visitStatementQuery(node, ctx)
    }

    override fun visitStatementQuery(node: Statement.Query, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitIdentifier(node: Identifier, ctx: C): R = when (node) {
        is Identifier.Symbol -> visitIdentifierSymbol(node, ctx)
        is Identifier.Qualified -> visitIdentifierQualified(node, ctx)
    }

    override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRex(node: Rex, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOp(node: Rex.Op, ctx: C): R = when (node) {
        is Rex.Op.Lit -> visitRexOpLit(node, ctx)
        is Rex.Op.Var -> visitRexOpVar(node, ctx)
        is Rex.Op.Global -> visitRexOpGlobal(node, ctx)
        is Rex.Op.Path -> visitRexOpPath(node, ctx)
        is Rex.Op.Call -> visitRexOpCall(node, ctx)
        is Rex.Op.Case -> visitRexOpCase(node, ctx)
        is Rex.Op.Collection -> visitRexOpCollection(node, ctx)
        is Rex.Op.Struct -> visitRexOpStruct(node, ctx)
        is Rex.Op.Pivot -> visitRexOpPivot(node, ctx)
        is Rex.Op.Subquery -> visitRexOpSubquery(node, ctx)
        is Rex.Op.Select -> visitRexOpSelect(node, ctx)
        is Rex.Op.TupleUnion -> visitRexOpTupleUnion(node, ctx)
        is Rex.Op.Err -> visitRexOpErr(node, ctx)
    }

    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOpVar(node: Rex.Op.Var, ctx: C): R = when (node) {
        is Rex.Op.Var.Resolved -> visitRexOpVarResolved(node, ctx)
        is Rex.Op.Var.Unresolved -> visitRexOpVarUnresolved(node, ctx)
    }

    override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOpPath(node: Rex.Op.Path, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOpPathStep(node: Rex.Op.Path.Step, ctx: C): R = when (node) {
        is Rex.Op.Path.Step.Index -> visitRexOpPathStepIndex(node, ctx)
        is Rex.Op.Path.Step.Key -> visitRexOpPathStepKey(node, ctx)
        is Rex.Op.Path.Step.Symbol -> visitRexOpPathStepSymbol(node, ctx)
        is Rex.Op.Path.Step.Wildcard -> visitRexOpPathStepWildcard(node, ctx)
        is Rex.Op.Path.Step.Unpivot -> visitRexOpPathStepUnpivot(node, ctx)
    }

    override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpPathStepKey(node: Rex.Op.Path.Step.Key, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpPathStepSymbol(node: Rex.Op.Path.Step.Symbol, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpCall(node: Rex.Op.Call, ctx: C): R = when (node) {
        is Rex.Op.Call.Static -> visitRexOpCallStatic(node, ctx)
        is Rex.Op.Call.Dynamic -> visitRexOpCallDynamic(node, ctx)
    }

    override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpCallDynamicCandidate(node: Rex.Op.Call.Dynamic.Candidate, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOpStructField(node: Rex.Op.Struct.Field, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitRexOpErr(node: Rex.Op.Err, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRel(node: Rel, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelType(node: Rel.Type, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOp(node: Rel.Op, ctx: C): R = when (node) {
        is Rel.Op.Scan -> visitRelOpScan(node, ctx)
        is Rel.Op.ScanIndexed -> visitRelOpScanIndexed(node, ctx)
        is Rel.Op.Unpivot -> visitRelOpUnpivot(node, ctx)
        is Rel.Op.Distinct -> visitRelOpDistinct(node, ctx)
        is Rel.Op.Filter -> visitRelOpFilter(node, ctx)
        is Rel.Op.Sort -> visitRelOpSort(node, ctx)
        is Rel.Op.Union -> visitRelOpUnion(node, ctx)
        is Rel.Op.Intersect -> visitRelOpIntersect(node, ctx)
        is Rel.Op.Except -> visitRelOpExcept(node, ctx)
        is Rel.Op.Limit -> visitRelOpLimit(node, ctx)
        is Rel.Op.Offset -> visitRelOpOffset(node, ctx)
        is Rel.Op.Project -> visitRelOpProject(node, ctx)
        is Rel.Op.Join -> visitRelOpJoin(node, ctx)
        is Rel.Op.Aggregate -> visitRelOpAggregate(node, ctx)
        is Rel.Op.Exclude -> visitRelOpExclude(node, ctx)
        is Rel.Op.Err -> visitRelOpErr(node, ctx)
    }

    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpSort(node: Rel.Op.Sort, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpSortSpec(node: Rel.Op.Sort.Spec, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitRelOpUnion(node: Rel.Op.Union, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitRelOpExcept(node: Rel.Op.Except, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    override fun visitRelOpAggregateCall(node: Rel.Op.Aggregate.Call, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelOpExcludeItem(node: Rel.Op.Exclude.Item, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRelOpExcludeStep(node: Rel.Op.Exclude.Step, ctx: C): R = when (node) {
        is Rel.Op.Exclude.Step.Attr -> visitRelOpExcludeStepAttr(node, ctx)
        is Rel.Op.Exclude.Step.Pos -> visitRelOpExcludeStepPos(node, ctx)
        is Rel.Op.Exclude.Step.StructWildcard -> visitRelOpExcludeStepStructWildcard(node, ctx)
        is Rel.Op.Exclude.Step.CollectionWildcard -> visitRelOpExcludeStepCollectionWildcard(node, ctx)
    }

    override fun visitRelOpExcludeStepAttr(node: Rel.Op.Exclude.Step.Attr, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRelOpExcludeStepPos(node: Rel.Op.Exclude.Step.Pos, ctx: C): R =
        defaultVisit(node, ctx)

    override fun visitRelOpExcludeStepStructWildcard(
        node: Rel.Op.Exclude.Step.StructWildcard,
        ctx: C,
    ): R = defaultVisit(node, ctx)

    override
    fun visitRelOpExcludeStepCollectionWildcard(
        node: Rel.Op.Exclude.Step.CollectionWildcard,
        ctx: C,
    ): R = defaultVisit(node, ctx)

    override fun visitRelOpErr(node: Rel.Op.Err, ctx: C): R = defaultVisit(node, ctx)

    override fun visitRelBinding(node: Rel.Binding, ctx: C): R = defaultVisit(node, ctx)

    internal open fun defaultVisit(node: PlanNode, ctx: C): R {
        for (child in node.children) {
            child.accept(this, ctx)
        }
        return defaultReturn(node, ctx)
    }

    internal abstract fun defaultReturn(node: PlanNode, ctx: C): R
}
