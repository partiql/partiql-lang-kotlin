package org.partiql.transpiler.sql

import org.partiql.ast.Ast
import org.partiql.ast.Expr
import org.partiql.ast.builder.AstFactory
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.TranspilerProblem
import org.partiql.ast.Identifier as AstIdentifier
import org.partiql.ast.Statement as AstStatement
import org.partiql.plan.Identifier as PlanIdentifier
import org.partiql.plan.Statement as PlanStatement

/**
 * [SqlTransform] represents extendable logic for translating from a [PlanNode] to [AstNode] tree.
 */
public open class SqlTransform(
    private val globals: List<Global>,
    private val calls: SqlCalls,
    private val onProblem: ProblemCallback,
) {

    public fun apply(statement: PlanStatement): AstStatement {
        if (statement is PlanStatement.Query) {
            val transform = RexToSql(this, emptyList())
            val expr = transform.apply(statement.root)
            return Ast.statementQuery(expr)
        }
        throw UnsupportedOperationException("Can only transform a query statement")
    }

    public fun getGlobal(ref: Int): AstIdentifier.Qualified? {
        val g = globals.getOrNull(ref) ?: return null
        return Ast.translate(g.path)
    }

    public fun getFunction(name: String, args: SqlArgs): Expr = calls.retarget(name, args)

    public fun handleProblem(problem: TranspilerProblem) = onProblem(problem)

    // Helpers

    companion object {

        public fun AstFactory.translate(identifier: PlanIdentifier): AstIdentifier = when (identifier) {
            is PlanIdentifier.Qualified -> translate(identifier)
            is PlanIdentifier.Symbol -> translate(identifier)
        }

        public fun AstFactory.translate(identifier: PlanIdentifier.Symbol): AstIdentifier.Symbol {
            return identifierSymbol(
                symbol = identifier.symbol,
                caseSensitivity = translate(identifier.caseSensitivity),
            )
        }

        public fun AstFactory.translate(identifier: PlanIdentifier.Qualified): AstIdentifier.Qualified {
            return identifierQualified(
                root = translate(identifier.root),
                steps = identifier.steps.map { translate(it) },
            )
        }

        public fun AstFactory.id(symbol: String): AstIdentifier.Symbol = identifierSymbol(
            symbol = symbol,
            caseSensitivity = AstIdentifier.CaseSensitivity.SENSITIVE,
        )

        private fun translate(case: Identifier.CaseSensitivity) = when (case) {
            PlanIdentifier.CaseSensitivity.SENSITIVE -> AstIdentifier.CaseSensitivity.SENSITIVE
            PlanIdentifier.CaseSensitivity.INSENSITIVE -> AstIdentifier.CaseSensitivity.INSENSITIVE
        }
    }
}
