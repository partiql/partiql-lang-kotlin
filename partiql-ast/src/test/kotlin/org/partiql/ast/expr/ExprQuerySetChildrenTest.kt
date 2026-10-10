package org.partiql.ast.expr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.ast.Ast
import org.partiql.ast.From
import org.partiql.ast.FromExpr
import org.partiql.ast.FromType
import org.partiql.ast.QueryBody

class ExprQuerySetChildrenTest {

    @Test
    fun getChildrenIncludesWithWhenPresent() {
        val cteBody = queryBodySfw()
        val withClause = Ast.with(
            listOf(Ast.withListElement(Ast.identifierSimple("x", true), Ast.exprQuerySet(cteBody), null)),
            false,
        )
        val querySet = Ast.exprQuerySet(queryBodySfw(), with = withClause)

        assertTrue(querySet.children.contains(withClause))
        assertEquals(withClause, querySet.children.first())
    }

    private fun queryBodySfw(): QueryBody = Ast.queryBodySFW(
        select = Ast.selectStar(),
        from = From(listOf(FromExpr(Ast.exprLit(org.partiql.ast.Literal.intNum(1)), FromType.SCAN(), null, null))),
    )
}
