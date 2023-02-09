package org.partiql.ir.rel.visitor

import org.partiql.ir.rel.Binding
import org.partiql.ir.rel.Common
import org.partiql.ir.rel.PlanNode
import org.partiql.ir.rel.Rel

public interface PlanVisitor<R, C> {
    public fun visit(node: PlanNode, ctx: C): R

    public fun visitCommon(node: Common, ctx: C): R

    public fun visitRel(node: Rel, ctx: C): R

    public fun visitRelScan(node: Rel.Scan, ctx: C): R

    public fun visitRelCross(node: Rel.Cross, ctx: C): R

    public fun visitRelFilter(node: Rel.Filter, ctx: C): R

    public fun visitRelSort(node: Rel.Sort, ctx: C): R

    public fun visitRelBag(node: Rel.Bag, ctx: C): R

    public fun visitRelFetch(node: Rel.Fetch, ctx: C): R

    public fun visitRelProject(node: Rel.Project, ctx: C): R

    public fun visitRelJoin(node: Rel.Join, ctx: C): R

    public fun visitRelAggregate(node: Rel.Aggregate, ctx: C): R

    public fun visitBinding(node: Binding, ctx: C): R
}
