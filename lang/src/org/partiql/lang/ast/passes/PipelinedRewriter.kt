package org.partiql.lang.ast.passes

import org.partiql.lang.ast.ExprNode

/**
 * A simple rewriter that provides a pipeline of rewrites.
 *
 * @param rewriters
 */
class PipelinedRewriter(vararg rewriters: AstRewriter) : AstRewriter {
    private val rewriterList =  rewriters.toList()

    override fun rewriteExprNode(originalNode: ExprNode): ExprNode =
        rewriterList.fold(originalNode) { intermediateNode, rewriter -> rewriter.rewriteExprNode(intermediateNode) }
}