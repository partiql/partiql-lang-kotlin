package org.partiql.planner.internal.transforms

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstRewriter
import org.partiql.ast.v1.expr.Expr

internal object SubstitutionVisitor : AstRewriter<Map<*, AstNode>>() {
    override fun visitExpr(node: Expr, ctx: Map<*, AstNode>): AstNode {
        val visited = super.visitExpr(node, ctx)
        if (ctx.containsKey(visited)) {
            return ctx[visited]!!
        }
        return visited
    }
}
