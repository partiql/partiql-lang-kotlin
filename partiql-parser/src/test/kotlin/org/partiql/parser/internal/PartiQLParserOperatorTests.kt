package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.v1.Ast.exprLit
import org.partiql.ast.v1.Ast.exprOperator
import org.partiql.ast.v1.Ast.query
import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.expr.Expr
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class PartiQLParserOperatorTests {

    private val parser = V1PartiQLParserDefault()

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
        val actual = result.root
        assertEquals(expected, actual)
    }
}
