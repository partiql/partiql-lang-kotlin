package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.Statement
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprOperator
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.sql.sql
import org.partiql.parser.PartiQLParser
import kotlin.test.assertEquals

/**
 * Printing an AST with [org.partiql.ast.sql.SqlDialect] must not let names or literal values built through the AST API
 * change the structure of the SQL: printing and re-parsing gives back the same AST.
 */
@Suppress("DEPRECATION")
class SqlDialectInjectionTests {

    private val parser = PartiQLParser.standard()

    private val payload = "x\" (1) OVER ()) AS s, (SELECT 'pwned' AS \"y"

    private fun parse(query: String): Statement = parser.parse(query).statements.single()

    private fun rewrite(statement: Statement, rewriter: AstRewriter<Unit>): Statement =
        rewriter.visitStatement(statement, Unit) as Statement

    private fun assertRoundTrip(statement: Statement) {
        val printed = statement.sql()
        assertEquals(statement, parse(printed), "Printed as: $printed")
    }

    private fun withAggregateName(function: Identifier) = rewrite(
        parse("SELECT SUM(t.x) OVER () AS s FROM t"),
        object : AstRewriter<Unit>() {
            override fun visitWindowFunctionTypeAggregate(node: WindowFunctionType.Aggregate, ctx: Unit): AstNode =
                WindowFunctionType.Aggregate(function, node.args, node.setq)
        }
    )

    // The grammar only accepts the aggregate keywords before OVER, so another aggregate name cannot re-parse; check
    // that the whole name stays inside one quoted identifier.
    private val quotedPayload = "\"" + payload.replace("\"", "\"\"") + "\""

    @Test
    fun delimitedAggregateNameIsQuoted() = assertEquals(
        "SELECT $quotedPayload(t.x) OVER () AS s FROM t",
        withAggregateName(Identifier.delimited(payload)).sql()
    )

    @Test
    fun qualifiedAggregateNameIsQuoted() = assertEquals(
        "SELECT \"demo\".$quotedPayload(t.x) OVER () AS s FROM t",
        withAggregateName(Identifier.delimited("demo", payload)).sql()
    )

    @Test
    fun regularAggregateNameThatIsNotAnIdentifierIsQuoted() = assertEquals(
        "SELECT $quotedPayload(t.x) OVER () AS s FROM t",
        withAggregateName(Identifier.regular(payload)).sql()
    )

    @Test
    fun keywordAggregateNameRoundTrips() = assertRoundTrip(withAggregateName(Identifier.regular("SUM")))

    @Test
    fun delimitedVariableAndAliasAreQuoted() = assertRoundTrip(
        rewrite(
            parse("SELECT t.x AS s FROM t AS t"),
            object : AstRewriter<Unit>() {
                override fun visitIdentifierSimple(node: Identifier.Simple, ctx: Unit): AstNode = Identifier.Simple.delimited(payload)
            }
        )
    )

    @Test
    fun stringLiteralsAreEscaped() = assertRoundTrip(
        rewrite(
            parse("SELECT 'a' AS s, DATE '2020-01-01' AS d FROM t"),
            object : AstRewriter<Unit>() {
                override fun visitExprLit(node: ExprLit, ctx: Unit): AstNode = when (node.lit.code()) {
                    Literal.STRING -> ExprLit(Literal.string("it's ' OR '1'='1"))
                    else -> node
                }
            }
        )
    )

    @Test
    fun unaryMinusBeforeNegativeLiteralIsNotAComment() {
        // `-(-1)` and `-(-x)`: the operand starts with `-`, which must not print as `--` (a comment).
        val negativeLiteral = rewrite(
            parse("SELECT -x AS s FROM t"),
            object : AstRewriter<Unit>() {
                override fun visitExprVarRef(node: ExprVarRef, ctx: Unit): AstNode = ExprLit(Literal.intNum("-1"))
            }
        )
        assertRoundTrip(negativeLiteral)
        val nested = rewrite(
            parse("SELECT -x AS s FROM t"),
            object : AstRewriter<Unit>() {
                override fun visitExprOperator(node: ExprOperator, ctx: Unit): AstNode = ExprOperator("-", null, ExprOperator("-", null, node.rhs))
            }
        )
        assertRoundTrip(nested)
    }

    @Test
    fun invalidNumericLiteralIsRejected() {
        val statement = rewrite(
            parse("SELECT 1 AS s FROM t"),
            object : AstRewriter<Unit>() {
                override fun visitExprLit(node: ExprLit, ctx: Unit): AstNode = ExprLit(Literal.intNum("1) OR (1=1"))
            }
        )
        assertThrows<IllegalArgumentException> { statement.sql() }
    }

    @Test
    fun invalidOperatorSymbolIsRejected() {
        listOf("+ 1) OR (1", "-- comment", "/* comment", "a").forEach { symbol ->
            val statement = rewrite(
                parse("SELECT t.x + t.y AS s FROM t"),
                object : AstRewriter<Unit>() {
                    override fun visitExprOperator(node: ExprOperator, ctx: Unit): AstNode = ExprOperator(symbol, node.lhs, node.rhs)
                }
            )
            assertThrows<IllegalArgumentException>("symbol: $symbol") { statement.sql() }
        }
    }
}
