package org.partiql.plan.ir.visitor

import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.StructPart

public abstract class PlanBaseVisitor<R, C> : PlanVisitor<R, C> {
    public override fun visit(node: PlanNode, ctx: C): R = node.accept(this, ctx)

    public override fun visitCommon(node: Common, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRel(node: Rel, ctx: C): R = when (node) {
        is Rel.Scan -> visitRelScan(node, ctx)
        is Rel.Filter -> visitRelFilter(node, ctx)
        is Rel.Sort -> visitRelSort(node, ctx)
        is Rel.Bag -> visitRelBag(node, ctx)
        is Rel.Fetch -> visitRelFetch(node, ctx)
        is Rel.Project -> visitRelProject(node, ctx)
        is Rel.Join -> visitRelJoin(node, ctx)
        is Rel.Aggregate -> visitRelAggregate(node, ctx)
    }

    public override fun visitRelScan(node: Rel.Scan, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRelFilter(node: Rel.Filter, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRelSort(node: Rel.Sort, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRelBag(node: Rel.Bag, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRelFetch(node: Rel.Fetch, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRelProject(node: Rel.Project, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRelJoin(node: Rel.Join, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRelAggregate(node: Rel.Aggregate, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRex(node: Rex, ctx: C): R = when (node) {
        is Rex.Id -> visitRexId(node, ctx)
        is Rex.Path -> visitRexPath(node, ctx)
        is Rex.Unary -> visitRexUnary(node, ctx)
        is Rex.Binary -> visitRexBinary(node, ctx)
        is Rex.Call -> visitRexCall(node, ctx)
        is Rex.Agg -> visitRexAgg(node, ctx)
        is Rex.Lit -> visitRexLit(node, ctx)
        is Rex.Collection -> visitRexCollection(node, ctx)
        is Rex.Struct -> visitRexStruct(node, ctx)
        is Rex.Subquery -> visitRexSubquery(node, ctx)
    }

    public override fun visitRexId(node: Rex.Id, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexPath(node: Rex.Path, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexUnary(node: Rex.Unary, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexBinary(node: Rex.Binary, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexCall(node: Rex.Call, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexAgg(node: Rex.Agg, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexLit(node: Rex.Lit, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexCollection(node: Rex.Collection, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexStruct(node: Rex.Struct, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitRexSubquery(node: Rex.Subquery, ctx: C): R = when (node) {
        is Rex.Subquery.Tuple -> visitRexSubqueryTuple(node, ctx)
        is Rex.Subquery.Scalar -> visitRexSubqueryScalar(node, ctx)
        is Rex.Subquery.Collection -> visitRexSubqueryCollection(node, ctx)
    }

    public override fun visitRexSubqueryTuple(node: Rex.Subquery.Tuple, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitRexSubqueryScalar(node: Rex.Subquery.Scalar, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitRexSubqueryCollection(node: Rex.Subquery.Collection, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStructPart(node: StructPart, ctx: C): R = when (node) {
        is StructPart.Fields -> visitStructPartFields(node, ctx)
        is StructPart.Field -> visitStructPartField(node, ctx)
    }

    public override fun visitStructPartFields(node: StructPart.Fields, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitStructPartField(node: StructPart.Field, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitSortSpec(node: SortSpec, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitBinding(node: Binding, ctx: C): R = defaultVisit(node, ctx)

    public open fun defaultVisit(node: PlanNode, ctx: C): R {
        for (child in node.children) {
            child.accept(this, ctx)
        }
        return defaultReturn(node, ctx)
    }

    public abstract fun defaultReturn(node: PlanNode, ctx: C): R
}
