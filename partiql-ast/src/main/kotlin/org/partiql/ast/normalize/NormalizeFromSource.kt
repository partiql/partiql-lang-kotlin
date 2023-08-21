package org.partiql.ast.normalize

import org.partiql.ast.AstNode
import org.partiql.ast.AstPass
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Statement
import org.partiql.ast.builder.ast
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.util.AstRewriter

/**
 * Assign aliases to any FROM source which does not have one.
 */
internal object NormalizeFromSource : AstPass {

    override fun apply(statement: Statement): Statement = Visitor.visitStatement(statement, 0) as Statement

    private object Visitor : AstRewriter<Int>() {

        override fun visitExprSFW(node: Expr.SFW, ctx: Int): AstNode = super.visitExprSFW(node, 0)

        override fun visitStatementDMLBatchLegacy(node: Statement.DML.BatchLegacy, ctx: Int): AstNode =
            super.visitStatementDMLBatchLegacy(node, 0)

        override fun visitFrom(node: From, ctx: Int) = super.visitFrom(node, ctx) as From

        override fun visitFromJoin(node: From.Join, ctx: Int) = ast {
            val lhs = visitFrom(node.lhs, ctx)
            val rhs = visitFrom(node.rhs, ctx + 1)
            val condition = node.condition?.let { visitExpr(it, ctx) as Expr }
            if (lhs !== node.lhs || rhs !== node.rhs || condition !== node.condition) {
                fromJoin(lhs, rhs, node.type, condition)
            } else {
                node
            }
        }

        override fun visitFromValue(node: From.Value, ctx: Int) = when (node.asAlias) {
            null -> node.copy(asAlias = node.expr.toBinder(ctx))
            else -> node
        }
    }
}
