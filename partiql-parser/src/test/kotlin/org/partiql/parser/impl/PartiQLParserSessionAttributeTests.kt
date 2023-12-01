package org.partiql.parser.impl

import org.junit.jupiter.api.Test
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.exprBinary
import org.partiql.ast.exprLit
import org.partiql.ast.exprSessionAttribute
import org.partiql.ast.statementQuery
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class PartiQLParserSessionAttributeTests {

    private val parser = PartiQLParserDefault()

    private inline fun query(body: () -> Expr) = statementQuery(body())

    @Test
    fun currentUserUpperCase() = assertExpression(
        "CURRENT_USER",
        query {
            exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_USER)
        }
    )

    @Test
    fun currentUserMixedCase() = assertExpression(
        "CURRENT_user",
        query {
            exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_USER)
        }
    )

    @Test
    fun currentUserLowerCase() = assertExpression(
        "current_user",
        query {
            exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_USER)
        }
    )

    @Test
    fun currentUserEquals() = assertExpression(
        "1 = current_user",
        query {
            exprBinary(
                op = Expr.Binary.Op.EQ,
                lhs = exprLit(int32Value(1)),
                rhs = exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_USER)
            )
        }
    )

    @Test
    fun currentDateUpperCase() = assertExpression(
        "CURRENT_DATE",
        query {
            exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_DATE)
        }
    )

    @Test
    fun currentDateMixedCase() = assertExpression(
        "CURRENT_date",
        query {
            exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_DATE)
        }
    )

    @Test
    fun currentDateLowerCase() = assertExpression(
        "current_date",
        query {
            exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_DATE)
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        val actual = result.root
        assertEquals(expected, actual)
    }
}
