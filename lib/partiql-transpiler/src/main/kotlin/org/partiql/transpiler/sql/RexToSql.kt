package org.partiql.transpiler.sql

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.builder.AstFactory
import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.transpiler.ProblemCallback

/**
 * RexToS
 *
 * @property onProblem  Invoked when a translation problem occurs
 */
public open class RexToSql(
    private val parent: SqlTransform,
    private val factory: AstFactory,
    private val onProblem: ProblemCallback,
) : PlanBaseVisitor<AstNode, Unit>() {

    private inline fun <T : AstNode> unplan(block: AstFactory.() -> T): T = factory.block()

    override fun defaultReturn(node: PlanNode, ctx: Unit): AstNode =
        throw UnsupportedOperationException("Cannot unplan $node")

    override fun visitRex(node: Rex, ctx: Unit) = super.visitRex(node, ctx) as Expr
}
