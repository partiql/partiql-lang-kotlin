package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst
import org.partiql.planner.ExperimentalPartiQLPlanner
import org.partiql.planner.transforms.FromSourceAliasVisitorTransform
import org.partiql.planner.transforms.OrderBySortSpecVisitorTransform
import org.partiql.planner.transforms.PipelinedVisitorTransform
import org.partiql.planner.transforms.SelectListItemAliasVisitorTransform
import org.partiql.planner.transforms.SelectStarVisitorTransform
import org.partiql.planner.transforms.SubqueryCoercionVisitorTransform
import org.partiql.planner.transforms.VisitorTransformBase

/** AST Normalization Passes.
 *
 * Returns a [PartiqlAst.VisitorTransform] requiring no external state for the basic functionality of compiling
 * PartiQL queries.
 *
 * Note that this is a function because some of the underlying visitor transforms are stateful.
 */
@OptIn(ExperimentalPartiQLPlanner::class)
fun basicVisitorTransforms() = PipelinedVisitorTransform(
    // These visitor transforms do not depend on each other and can be executed in any order.
    SelectListItemAliasVisitorTransform(),
    FromSourceAliasVisitorTransform(),
    GroupByItemAliasVisitorTransform(),
    AggregateSupportVisitorTransform(),
    OrderBySortSpecVisitorTransform(),

    // [GroupByPathExpressionVisitorTransform] and [SelectStarVisitorTransform] require:
    //   - the synthetic from source aliases added by [FromSourceAliasVisitorTransform]
    //   - The synthetic group by item aliases added by [GroupByItemAliasVisitorTransform]
    GroupByPathExpressionVisitorTransform(),
    SelectStarVisitorTransform(),

    SubqueryCoercionVisitorTransform(),
)

/** A stateless visitor transform that returns the input. */
@JvmField
internal val IDENTITY_VISITOR_TRANSFORM: PartiqlAst.VisitorTransform = object : VisitorTransformBase() {
    override fun transformStatement(node: PartiqlAst.Statement): PartiqlAst.Statement = node
}
