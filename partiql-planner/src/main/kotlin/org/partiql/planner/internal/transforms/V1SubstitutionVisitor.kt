package org.partiql.planner.internal.transforms

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.expr.Expr

internal object V1SubstitutionVisitor : AstVisitor<AstNode, Map<*, AstNode>> {
    override fun defaultReturn(node: AstNode, ctx: Map<*, AstNode>) = node

    override fun visitExpr(node: Expr, ctx: Map<*, AstNode>): AstNode {
        val visited = super.visitExpr(node, ctx)
        if (ctx.containsKey(visited)) {
            return ctx[visited]!!
        }
        return visited
    }
}
