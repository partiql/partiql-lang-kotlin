package org.partiql.parser.internal

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.ast.AstVisitor
import org.partiql.ast.FromExpr
import org.partiql.ast.Query
import org.partiql.ast.expr.ExprVarRef

class ExprQuerySetWithTraversalTest {

    private val parser = PartiQLParserDefault()

    @Test
    fun defaultVisitorTraversesCteQueryBody() {
        val statement = parser.parse("WITH x AS (SELECT * FROM t) SELECT * FROM x").statements.single() as Query

        val tableRefs = mutableListOf<FromExpr>()
        val varRefs = mutableSetOf<String>()
        val visitor = object : AstVisitor<Unit, Unit>() {
            override fun defaultReturn(node: org.partiql.ast.AstNode, ctx: Unit) = Unit

            override fun visitFromExpr(node: FromExpr, ctx: Unit) {
                tableRefs.add(node)
                super.visitFromExpr(node, ctx)
            }

            override fun visitExprVarRef(node: ExprVarRef, ctx: Unit) {
                varRefs.add(node.identifier.identifier.text)
                super.visitExprVarRef(node, ctx)
            }
        }
        statement.accept(visitor, Unit)

        assertTrue("t" in varRefs, "Expected table reference `t` from the CTE body; got $varRefs")
        assertTrue(tableRefs.isNotEmpty(), "Expected at least one FromExpr from traversal")
    }
}
