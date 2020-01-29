package org.partiql.lang.ast.passes

import org.partiql.lang.ast.*

/**
 * Returns an [AstRewriter] requiring no external state for the basic functionality of compiling
 * PartiQL queries.
 *
 * Note that this is a function because some of the underlying rewriters are stateful.
 */
fun basicRewriters() = PipelinedRewriter(
    // These rewriters do not depend on each other and can be executed in any order.
    SelectListItemAliasRewriter(),
    FromSourceAliasRewriter(),
    GroupByItemAliasRewriter(),
    AggregateSupportRewriter(),

    // [GroupByPathExpressionRewriter] requires:
    //   - the synthetic from source aliases added by [FromSourceAliasRewriter]
    //   - The synthetic group by item aliases added by [GroupByItemAliasRewriter]
    GroupByPathExpressionRewriter(),
    SelectStarRewriter()
)

/** A stateless rewriter that returns the input. */
@JvmField
internal val IDENTITY_REWRITER: AstRewriter = object : AstRewriter{
    override fun rewriteExprNode(node: ExprNode): ExprNode = node
}
