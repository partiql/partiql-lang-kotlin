package org.partiql.lang.ast.passes

import com.amazon.ion.IonSystem
import org.partiql.lang.ast.*
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.SelectListItemAliasTransform

/**
 * Returns an [AstRewriter] requiring no external state for the basic functionality of compiling
 * PartiQL queries.
 *
 * Note that this is a function because some of the underlying rewriters are stateful.
 */
fun basicRewriters(ion: IonSystem) = PipelinedRewriter(
    // These rewriters do not depend on each other and can be executed in any order.
    ExprNodeWrappingRewriter(SelectListItemAliasTransform(), ion),
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
// TODO:  move to another file
class ExprNodeWrappingRewriter(val transform: PartiqlAst.VisitorTransform, val ion: IonSystem) : AstRewriter {
    override fun rewriteExprNode(node: ExprNode): ExprNode {
        val pigAst = node.toAstStatement()
        val result = transform.transformStatement(pigAst)
        val en = result.toExprNode(ion)
        return en
    }

}