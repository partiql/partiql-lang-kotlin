package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.expr.Expr

internal object SubstitutionVisitor : AstRewriter<Map<*, AstNode>>() {
    override fun visitExpr(node: Expr, ctx: Map<*, AstNode>): AstNode {
        val visited = super.visitExpr(node, ctx)
        if (ctx.containsKey(visited)) {
            return ctx[visited]!!
        }
        return visited
    }
}
