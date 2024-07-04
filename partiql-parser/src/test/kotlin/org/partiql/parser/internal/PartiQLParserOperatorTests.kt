package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.exprLit
import org.partiql.ast.exprOperator
import org.partiql.ast.statementQuery
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class PartiQLParserOperatorTests {

    private val parser = PartiQLParserDefault()

    private inline fun query(body: () -> Expr) = statementQuery(body())

    @Test
    fun builtinUnaryOperator() = assertExpression(
        "-2",
        query {
            exprOperator(
                symbol = "-",
                lhs = null,
                rhs = exprLit(int32Value(2))
            )
        }
    )

    @Test
    fun builtinBinaryOperator() = assertExpression(
        "1 <= 2",
        query {
            exprOperator(
                symbol = "<=",
                lhs = exprLit(int32Value(1)),
                rhs = exprLit(int32Value(2))
            )
        }
    )

    @Test
    fun customUnaryOperator() = assertExpression(
        "==!2",
        query {
            exprOperator(
                symbol = "==!",
                lhs = null,
                rhs = exprLit(int32Value(2))
            )
        }
    )

    @Test
    fun customBinaryOperator() = assertExpression(
        "1 ==! 2",
        query {
            exprOperator(
                symbol = "==!",
                lhs = exprLit(int32Value(1)),
                rhs = exprLit(int32Value(2))
            )
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        val actual = result.root
        assertEquals(expected, actual)
    }
}
