package org.partiql.ast.normalize

import org.partiql.ast.Expr
import org.partiql.ast.GroupBy
import org.partiql.ast.Statement
import org.partiql.ast.groupByKey
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.util.AstRewriter

/**
 * Adds a unique binder to each group key.
 */
object NormalizeGroupBy : AstPass {

    override fun apply(statement: Statement) = Visitor.visitStatement(statement, 0) as Statement

    private object Visitor : AstRewriter<Int>() {

        override fun visitGroupByKey(node: GroupBy.Key, ctx: Int): GroupBy.Key {
            val expr = visitExpr(node.expr, 0) as Expr
            val alias = when (node.asAlias) {
                null -> expr.toBinder(ctx)
                else -> node.asAlias
            }
            return if (expr !== node.expr || alias !== node.asAlias) {
                groupByKey(expr, alias)
            } else {
                node
            }
        }
    }
}
