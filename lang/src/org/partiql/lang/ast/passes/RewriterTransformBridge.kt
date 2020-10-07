package org.partiql.lang.ast.passes

import com.amazon.ion.IonSystem
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst

/**
 * This is a temporary rewriter that converts the [ExprNode] AST to the PIG-generated [PartiqlAst],
 * runs the specified [PartiqlAst.VisitorTransform] on it, converts the result back to [ExprNode].
 *
 * This is only to be used on the `visitor-transforms` feature branch and will be removed once all of the
 * [AstRewriterBase] implementations have been migrated to [PartiqlAst.VisitorTransform].
 */
internal class RewriterTransformBridge(
    private val transform: PartiqlAst.VisitorTransform,
    private val ion: IonSystem
) : AstRewriter {

    override fun rewriteExprNode(node: ExprNode): ExprNode {
        val ast = node.toAstStatement()
        val transformed = transform.transformStatement(ast)
        return transformed.toExprNode(ion)

    }

}
