package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprSessionAttribute
import org.partiql.ast.Ast.query
import org.partiql.ast.AstNode
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.SessionAttribute
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class PartiQLParserSessionAttributeTests {

    private val parser = PartiQLParserDefault()

    private inline fun queryBody(body: () -> Expr) = query(body())

    @Test
    fun currentUserUpperCase() = assertExpression(
        "CURRENT_USER",
        queryBody {
            exprSessionAttribute(SessionAttribute.CURRENT_USER())
        }
    )

    @Test
    fun currentUserMixedCase() = assertExpression(
        "CURRENT_user",
        queryBody {
            exprSessionAttribute(SessionAttribute.CURRENT_USER())
        }
    )

    @Test
    fun currentUserLowerCase() = assertExpression(
        "current_user",
        queryBody {
            exprSessionAttribute(SessionAttribute.CURRENT_USER())
        }
    )

    @Test
    fun currentUserEquals() = assertExpression(
        "1 = current_user",
        queryBody {
            exprOperator(
                symbol = "=",
                lhs = exprLit(int32Value(1)),
                rhs = exprSessionAttribute(SessionAttribute.CURRENT_USER())
            )
        }
    )

    @Test
    fun currentDateUpperCase() = assertExpression(
        "CURRENT_DATE",
        queryBody {
            exprSessionAttribute(SessionAttribute.CURRENT_DATE())
        }
    )

    @Test
    fun currentDateMixedCase() = assertExpression(
        "CURRENT_date",
        queryBody {
            exprSessionAttribute(SessionAttribute.CURRENT_DATE())
        }
    )

    @Test
    fun currentDateLowerCase() = assertExpression(
        "current_date",
        queryBody {
            exprSessionAttribute(SessionAttribute.CURRENT_DATE())
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        assertEquals(1, result.statements.size)
        val actual = result.statements[0]
        assertEquals(expected, actual)
    }
}
