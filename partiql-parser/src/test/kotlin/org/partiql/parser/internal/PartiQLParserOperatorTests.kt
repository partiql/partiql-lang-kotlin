package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.query
import org.partiql.ast.AstNode
import org.partiql.ast.expr.Expr
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class PartiQLParserOperatorTests {

    private val parser = PartiQLParserDefault()

    private inline fun queryBody(body: () -> Expr) = query(body())

    @Test
    fun builtinUnaryOperator() = assertExpression(
        "-2",
        queryBody {
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
        queryBody {
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
        queryBody {
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
        queryBody {
            exprOperator(
                symbol = "==!",
                lhs = exprLit(int32Value(1)),
                rhs = exprLit(int32Value(2))
            )
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        assertEquals(1, result.statements.size)
        val actual = result.statements[0]
        assertEquals(expected, actual)
    }
}
