package org.partiql.plan.ir.visitor

import org.partiql.plan.ir.Attribute
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Field
import org.partiql.plan.ir.Plan
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.Step

public abstract class PlanBaseVisitor<R, C> : PlanVisitor<R, C> {
  public override fun visit(node: PlanNode, ctx: C): R = node.accept(this, ctx)

  public override fun visitPlan(node: Plan, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitCommon(node: Common, ctx: C): R = defaultVisit(node, ctx)

<<<<<<< HEAD
  public override fun visitBinding(node: Binding, ctx: C): R = defaultVisit(node, ctx)
=======
    public override fun visitAttribute(node: Attribute, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitBinding(node: Binding, ctx: C): R = defaultVisit(node, ctx)
>>>>>>> 780d0657 (Adds SPI framework, schema inference, cli tool, localdb connector, and plan typing)

  public override fun visitField(node: Field, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitStep(node: Step, ctx: C): R = when (node) {
    is Step.Key -> visitStepKey(node, ctx)
    is Step.Wildcard -> visitStepWildcard(node, ctx)
    is Step.Unpivot -> visitStepUnpivot(node, ctx)
  }

  public override fun visitStepKey(node: Step.Key, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitStepWildcard(node: Step.Wildcard, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitStepUnpivot(node: Step.Unpivot, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitSortSpec(node: SortSpec, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRel(node: Rel, ctx: C): R = when (node) {
    is Rel.Scan -> visitRelScan(node, ctx)
    is Rel.Unpivot -> visitRelUnpivot(node, ctx)
    is Rel.Filter -> visitRelFilter(node, ctx)
    is Rel.Sort -> visitRelSort(node, ctx)
    is Rel.Bag -> visitRelBag(node, ctx)
    is Rel.Fetch -> visitRelFetch(node, ctx)
    is Rel.Project -> visitRelProject(node, ctx)
    is Rel.Join -> visitRelJoin(node, ctx)
    is Rel.Aggregate -> visitRelAggregate(node, ctx)
  }

  public override fun visitRelScan(node: Rel.Scan, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRelUnpivot(node: Rel.Unpivot, ctx: C): R = defaultVisit(node, ctx)

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
    is Rex.Tuple -> visitRexTuple(node, ctx)
    is Rex.Query -> visitRexQuery(node, ctx)
  }

  public override fun visitRexId(node: Rex.Id, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexPath(node: Rex.Path, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexUnary(node: Rex.Unary, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexBinary(node: Rex.Binary, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexCall(node: Rex.Call, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexAgg(node: Rex.Agg, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexLit(node: Rex.Lit, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexCollection(node: Rex.Collection, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexTuple(node: Rex.Tuple, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexQuery(node: Rex.Query, ctx: C): R = when (node) {
    is Rex.Query.Scalar -> visitRexQueryScalar(node, ctx)
    is Rex.Query.Collection -> visitRexQueryCollection(node, ctx)
  }

  public override fun visitRexQueryScalar(node: Rex.Query.Scalar, ctx: C): R = when (node) {
    is Rex.Query.Scalar.Coerce -> visitRexQueryScalarCoerce(node, ctx)
    is Rex.Query.Scalar.Pivot -> visitRexQueryScalarPivot(node, ctx)
  }

  public override fun visitRexQueryScalarCoerce(node: Rex.Query.Scalar.Coerce, ctx: C): R =
      defaultVisit(node, ctx)

  public override fun visitRexQueryScalarPivot(node: Rex.Query.Scalar.Pivot, ctx: C): R =
      defaultVisit(node, ctx)

  public override fun visitRexQueryCollection(node: Rex.Query.Collection, ctx: C): R =
      defaultVisit(node, ctx)

  public open fun defaultVisit(node: PlanNode, ctx: C): R {
    for (child in node.children) {
      child.accept(this, ctx)
    }
    return defaultReturn(node, ctx)
  }

  public abstract fun defaultReturn(node: PlanNode, ctx: C): R
}
