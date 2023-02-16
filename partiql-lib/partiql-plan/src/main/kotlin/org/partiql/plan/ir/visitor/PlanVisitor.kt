package org.partiql.plan.ir.visitor

import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.StructPart

public interface PlanVisitor<R, C> {
  public fun visit(node: PlanNode, ctx: C): R

  public fun visitCommon(node: Common, ctx: C): R

  public fun visitRel(node: Rel, ctx: C): R

  public fun visitRelScan(node: Rel.Scan, ctx: C): R

  public fun visitRelFilter(node: Rel.Filter, ctx: C): R

  public fun visitRelSort(node: Rel.Sort, ctx: C): R

  public fun visitRelBag(node: Rel.Bag, ctx: C): R

  public fun visitRelFetch(node: Rel.Fetch, ctx: C): R

  public fun visitRelProject(node: Rel.Project, ctx: C): R

  public fun visitRelJoin(node: Rel.Join, ctx: C): R

  public fun visitRelAggregate(node: Rel.Aggregate, ctx: C): R

  public fun visitRex(node: Rex, ctx: C): R

  public fun visitRexId(node: Rex.Id, ctx: C): R

  public fun visitRexPath(node: Rex.Path, ctx: C): R

  public fun visitRexUnary(node: Rex.Unary, ctx: C): R

  public fun visitRexBinary(node: Rex.Binary, ctx: C): R

  public fun visitRexCall(node: Rex.Call, ctx: C): R

  public fun visitRexAgg(node: Rex.Agg, ctx: C): R

  public fun visitRexLit(node: Rex.Lit, ctx: C): R

  public fun visitRexCollection(node: Rex.Collection, ctx: C): R

  public fun visitRexStruct(node: Rex.Struct, ctx: C): R

  public fun visitRexSubquery(node: Rex.Subquery, ctx: C): R

  public fun visitRexSubqueryTuple(node: Rex.Subquery.Tuple, ctx: C): R

  public fun visitRexSubqueryScalar(node: Rex.Subquery.Scalar, ctx: C): R

  public fun visitStructPart(node: StructPart, ctx: C): R

  public fun visitStructPartFields(node: StructPart.Fields, ctx: C): R

  public fun visitStructPartField(node: StructPart.Field, ctx: C): R

  public fun visitSortSpec(node: SortSpec, ctx: C): R

  public fun visitBinding(node: Binding, ctx: C): R
}
