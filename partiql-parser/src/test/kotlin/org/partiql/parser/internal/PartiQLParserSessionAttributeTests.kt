package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.v1.Ast.exprLit
import org.partiql.ast.v1.Ast.exprOperator
import org.partiql.ast.v1.Ast.exprSessionAttribute
import org.partiql.ast.v1.Ast.query
import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.expr.Expr
import org.partiql.ast.v1.expr.SessionAttribute
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class PartiQLParserSessionAttributeTests {

    private val parser = V1PartiQLParserDefault()

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
        val actual = result.root
        assertEquals(expected, actual)
    }
}
