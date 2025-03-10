package org.partiql.planner.internal.transforms

import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.Identifier
import org.partiql.ast.QueryBody
import org.partiql.ast.expr.ExprWindowFunction

/**
 * Rewrites SFW replacing (and extracting) each window function `i` with a synthetic field name `$window_func_i`.
 * TODO: Limit to just SELECT and ORDER BY.
 */
internal object WindowTransform : AstRewriter<WindowTransform.Context>() {
    internal data class Context(
        val functions: MutableList<Pair<String, ExprWindowFunction>>,
    )

    private fun syntheticName(i: Int) = "\$window_func_$i"

    /**
     * @return the rewritten SFW and a list of window functions with their binding names
     */
    fun apply(node: QueryBody.SFW): Pair<QueryBody.SFW, List<Pair<String, ExprWindowFunction>>> {
        val functions = mutableListOf<Pair<String, ExprWindowFunction>>()
        val context = Context(functions)
        val select = super.visitQueryBodySFW(node, context) as QueryBody.SFW
        return Pair(select, functions)
    }

    // only rewrite top-level SFW
    override fun visitQueryBodySFW(node: QueryBody.SFW, ctx: Context): AstNode = node

    override fun visitExprWindowFunction(node: ExprWindowFunction, ctx: Context): AstNode {
        val name = syntheticName(ctx.functions.size)
        val id = Identifier.delimited(name)
        ctx.functions += (name to node)
        return exprVarRef(id, isQualified = false)
    }

    override fun defaultReturn(node: AstNode, context: Context) = node
}
