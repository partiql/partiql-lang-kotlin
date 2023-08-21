package org.partiql.planner.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.plan.builder.PlanFactory
import org.partiql.planner.Env
import org.partiql.ast.Identifier as AstIdentifier
import org.partiql.ast.Statement as AstStatement
import org.partiql.plan.Identifier as PlanIdentifier
import org.partiql.plan.Statement as PlanStatement

/**
 * Simple translation from AST to an unresolved algebraic IR.
 */
internal object AstToPlan {

    // statement.toPlan()
    @JvmStatic
    fun apply(statement: AstStatement, env: Env): PlanStatement = statement.accept(ToPlanStatement, env)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToPlanStatement : AstBaseVisitor<PlanStatement, Env>() {

        private val factory = Plan

        private inline fun <T : PlanNode> transform(block: PlanFactory.() -> T): T = factory.block()

        override fun defaultReturn(node: AstNode, env: Env) = throw IllegalArgumentException("Unsupported statement")

        override fun visitStatementQuery(node: AstStatement.Query, env: Env) = transform {
            val rex = when (val expr = node.expr) {
                is Expr.SFW -> RelConverter.apply(expr, env)
                else -> RexConverter.apply(expr, env)
            }
            statementQuery(rex)
        }
    }

    // --- Helpers --------------------

    fun convert(identifier: AstIdentifier): PlanIdentifier = when (identifier) {
        is AstIdentifier.Qualified -> convert(identifier)
        is AstIdentifier.Symbol -> convert(identifier)
    }

    fun convert(identifier: AstIdentifier.Qualified): PlanIdentifier.Qualified {
        val root = convert(identifier.root)
        val steps = identifier.steps.map { convert(it) }
        return Plan.identifierQualified(root, steps)
    }

    fun convert(identifier: AstIdentifier.Symbol): PlanIdentifier.Symbol {
        val symbol = identifier.symbol
        val case = when (identifier.caseSensitivity) {
            AstIdentifier.CaseSensitivity.SENSITIVE -> PlanIdentifier.CaseSensitivity.SENSITIVE
            AstIdentifier.CaseSensitivity.INSENSITIVE -> PlanIdentifier.CaseSensitivity.INSENSITIVE
        }
        return Plan.identifierSymbol(symbol, case)
    }
}
