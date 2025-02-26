package org.partiql.planner.internal.transforms

import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.Identifier
import org.partiql.ast.OrderBy
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.SelectPivot
import org.partiql.ast.SelectValue
import org.partiql.ast.Sort
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprWindowFunction

/**
 * Rewrites SFW replacing (and extracting) each window function `i` with a synthetic field name `$window_func_i`.
 * This is intentionally limited to only replacing window functions in the SELECT/PIVOT and ORDER BY clause.
 * If the SFW is nested within another SFW, it will not be rewritten. This transform should be called manually on each.
 */
internal object WindowTransform : AstRewriter<WindowTransform.Context>() {

    internal data class Context(val functions: MutableList<Pair<String, ExprWindowFunction>>)

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

    override fun visitSelectItemExpr(node: SelectItem.Expr, ctx: Context): AstNode {
        val expr = WindowFunctionVisitor.visitExpr(node.expr, ctx) as Expr
        return SelectItem.Expr(expr, node.asAlias)
    }

    override fun visitSelectPivot(node: SelectPivot, ctx: Context): AstNode {
        val key = WindowFunctionVisitor.visitExpr(node.key, ctx) as Expr
        val value = WindowFunctionVisitor.visitExpr(node.value, ctx) as Expr
        return SelectPivot(key, value)
    }

    override fun visitSelectValue(node: SelectValue, ctx: Context): AstNode {
        val expr = WindowFunctionVisitor.visitExpr(node.constructor, ctx) as Expr
        return SelectValue(expr, node.setq)
    }

    override fun visitOrderBy(node: OrderBy, ctx: Context): AstNode {
        val sorts = node.sorts.map { sort ->
            val expr = WindowFunctionVisitor.visitExpr(sort.expr, ctx) as Expr
            Sort(expr, sort.order, sort.nulls)
        }
        return OrderBy(sorts)
    }

    // only rewrite top-level SFW
    override fun visitQueryBodySFW(node: QueryBody.SFW, ctx: Context): AstNode = node

    /**
     * This does the actual replacement.
     */
    private object WindowFunctionVisitor : AstRewriter<Context>() {

        override fun defaultReturn(node: AstNode, context: Context) = node

        override fun visitExprWindowFunction(node: ExprWindowFunction, ctx: Context): AstNode {
            val name = syntheticName(ctx.functions.size)
            val id = Identifier.delimited(name)
            ctx.functions += (name to node)
            return exprVarRef(id, isQualified = false)
        }

        // only rewrite top-level SFW
        override fun visitQueryBodySFW(node: QueryBody.SFW, ctx: Context): AstNode = node
    }
}
