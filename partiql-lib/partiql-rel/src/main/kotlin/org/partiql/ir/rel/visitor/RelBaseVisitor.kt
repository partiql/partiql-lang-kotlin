package org.partiql.ir.rel.visitor

import org.partiql.ir.rel.Binding
import org.partiql.ir.rel.Common
import org.partiql.ir.rel.Rel
import org.partiql.ir.rel.RelNode
import org.partiql.ir.rel.SortSpec

public abstract class RelBaseVisitor<R, C> : RelVisitor<R, C> {
  public override fun visit(node: RelNode, ctx: C): R = node.accept(this, ctx)

  public override fun visitCommon(node: Common, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRel(node: Rel, ctx: C): R = when (node) {
    is Rel.Scan -> visitRelScan(node, ctx)
    is Rel.Cross -> visitRelCross(node, ctx)
    is Rel.Filter -> visitRelFilter(node, ctx)
    is Rel.Sort -> visitRelSort(node, ctx)
    is Rel.Bag -> visitRelBag(node, ctx)
    is Rel.Fetch -> visitRelFetch(node, ctx)
    is Rel.Project -> visitRelProject(node, ctx)
    is Rel.Join -> visitRelJoin(node, ctx)
    is Rel.Aggregate -> visitRelAggregate(node, ctx)
  }

  public override fun visitRelScan(node: Rel.Scan, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelCross(node: Rel.Cross, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelFilter(node: Rel.Filter, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelSort(node: Rel.Sort, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelBag(node: Rel.Bag, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelFetch(node: Rel.Fetch, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelProject(node: Rel.Project, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelJoin(node: Rel.Join, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelAggregate(node: Rel.Aggregate, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitSortSpec(node: SortSpec, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitBinding(node: Binding, ctx: C): R = defaultVisit(node, ctx)

  public open fun defaultVisit(node: RelNode, ctx: C): R {
    for (child in node.children) {
      child.accept(this, ctx)
    }
    return defaultReturn(node, ctx)
  }

  public abstract fun defaultReturn(node: RelNode, ctx: C): R
}
