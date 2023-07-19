package org.partiql.ast.normalize

import org.partiql.ast.AstPass
import org.partiql.ast.Expr
import org.partiql.ast.Select
import org.partiql.ast.Statement
import org.partiql.ast.builder.ast
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.util.AstRewriter

/**
 * Adds an `as` alias to every select-list item following the given rules:
 *
 * See https://partiql.org/assets/PartiQL-Specification.pdf#page=28
 * See https://web.cecs.pdx.edu/~len/sql1999.pdf#page=287
 */
internal object NormalizeSelectList : AstPass {

    override fun apply(statement: Statement) = Visitor.visitStatement(statement, 0) as Statement

    private object Visitor : AstRewriter<Int>() {

        override fun visitSelectProject(node: Select.Project, ctx: Int) = ast {
            if (node.items.isEmpty()) {
                return@ast node
            }
            var diff = false
            val transformed = ArrayList<Select.Project.Item>(node.items.size)
            node.items.forEachIndexed { i, n ->
                val item = visitSelectProjectItem(n, i)
                if (item !== n) diff = true
                transformed.add(n)
            }
            // We don't want to create a new list unless we have to, as to not trigger further rewrites up the tree.
            if (diff) selectProject(transformed) else node
        }

        override fun visitSelectProjectItemAll(node: Select.Project.Item.All, ctx: Int) = node.copy()

        override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, ctx: Int) = ast {
            val expr = visitExpr(node.expr, 0) as Expr
            val alias = when (node.asAlias) {
                null -> expr.toBinder(ctx)
                else -> node.asAlias
            }
            if (expr != node.expr || alias != node.asAlias) {
                node.copy(asAlias = alias)
            } else {
                node
            }
        }
    }
}
