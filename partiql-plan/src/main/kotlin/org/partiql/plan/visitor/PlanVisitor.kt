package org.partiql.plan.visitor

import org.partiql.plan.Arg
import org.partiql.plan.Binding
import org.partiql.plan.Branch
import org.partiql.plan.Common
import org.partiql.plan.Field
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.SortSpec
import org.partiql.plan.Step

public interface PlanVisitor<R, C> {
    public fun visit(node: PlanNode, ctx: C): R

    public fun visitPartiQLPlan(node: PartiQLPlan, ctx: C): R

    public fun visitCommon(node: Common, ctx: C): R

    public fun visitBinding(node: Binding, ctx: C): R

    public fun visitField(node: Field, ctx: C): R

    public fun visitStep(node: Step, ctx: C): R

    public fun visitStepKey(node: Step.Key, ctx: C): R

    public fun visitStepWildcard(node: Step.Wildcard, ctx: C): R

    public fun visitStepUnpivot(node: Step.Unpivot, ctx: C): R

    public fun visitSortSpec(node: SortSpec, ctx: C): R

    public fun visitArg(node: Arg, ctx: C): R

    public fun visitArgValue(node: Arg.Value, ctx: C): R

    public fun visitArgType(node: Arg.Type, ctx: C): R

    public fun visitBranch(node: Branch, ctx: C): R

    public fun visitRel(node: Rel, ctx: C): R

    public fun visitRelScan(node: Rel.Scan, ctx: C): R

    public fun visitRelUnpivot(node: Rel.Unpivot, ctx: C): R

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

    public fun visitRexLit(node: Rex.Lit, ctx: C): R

    public fun visitRexUnary(node: Rex.Unary, ctx: C): R

    public fun visitRexBinary(node: Rex.Binary, ctx: C): R

    public fun visitRexCall(node: Rex.Call, ctx: C): R

    public fun visitRexSwitch(node: Rex.Switch, ctx: C): R

    public fun visitRexAgg(node: Rex.Agg, ctx: C): R

    public fun visitRexCollection(node: Rex.Collection, ctx: C): R

    public fun visitRexCollectionArray(node: Rex.Collection.Array, ctx: C): R

    public fun visitRexCollectionBag(node: Rex.Collection.Bag, ctx: C): R

    public fun visitRexTuple(node: Rex.Tuple, ctx: C): R

    public fun visitRexQuery(node: Rex.Query, ctx: C): R

    public fun visitRexQueryScalar(node: Rex.Query.Scalar, ctx: C): R

    public fun visitRexQueryScalarSubquery(node: Rex.Query.Scalar.Subquery, ctx: C): R

    public fun visitRexQueryScalarPivot(node: Rex.Query.Scalar.Pivot, ctx: C): R

    public fun visitRexQueryCollection(node: Rex.Query.Collection, ctx: C): R
}
