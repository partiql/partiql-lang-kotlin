package org.partiql.lang.ast.passes

import com.amazon.ion.IonSystem
import org.partiql.lang.ast.*
import org.partiql.lang.eval.visitors.AggregateSupportVisitorTransform
import org.partiql.lang.eval.visitors.FromSourceAliasVisitorTransform
import org.partiql.lang.eval.visitors.SelectListItemAliasVisitorTransform

/**
 * Returns an [AstRewriter] requiring no external state for the basic functionality of compiling
 * PartiQL queries.
 *
 * Note that this is a function because some of the underlying rewriters are stateful.
 */
fun basicRewriters(ion: IonSystem) = PipelinedRewriter(
    // These rewriters do not depend on each other and can be executed in any order.
    RewriterTransformBridge(SelectListItemAliasVisitorTransform(), ion),
    RewriterTransformBridge(FromSourceAliasVisitorTransform(), ion),
    GroupByItemAliasRewriter(),
    RewriterTransformBridge(AggregateSupportVisitorTransform(), ion),

    // [GroupByPathExpressionRewriter] requires:
    //   - the synthetic from source aliases added by [FromSourceAliasVisitorTransform]
    //   - The synthetic group by item aliases added by [GroupByItemAliasRewriter]
    GroupByPathExpressionRewriter(),
    SelectStarRewriter()
)

/** A stateless rewriter that returns the input. */
@JvmField
internal val IDENTITY_REWRITER: AstRewriter = object : AstRewriter{
    override fun rewriteExprNode(node: ExprNode): ExprNode = node
}
