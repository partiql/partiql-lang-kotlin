package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.TestCatalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import kotlin.test.assertEquals

class LowerTest {

    @Test
    fun `lower preserves CHAR length`() {
        val session = Session.builder()
            .catalog("default")
            .catalogs(
                TestCatalog.builder()
                    .name("default")
                    .build()
            )
            .build()
        val parseResult = PartiQLParser.standard().parse("LOWER(CAST('hello' AS CHAR(5)))")
        val ast = parseResult.statements[0]
        val planner = PartiQLPlanner.builder().build()
        val result = planner.plan(ast, session)
        val query = result.plan.action as Action.Query
        val actualType = query.rex.type.pType
        assertEquals(PType.CHAR, actualType.code())
        assertEquals(5, actualType.length)
    }

    @Test
    fun `lower preserves VARCHAR length`() {
        val session = Session.builder()
            .catalog("default")
            .catalogs(
                TestCatalog.builder()
                    .name("default")
                    .build()
            )
            .build()
        val parseResult = PartiQLParser.standard().parse("LOWER(CAST('hello' AS VARCHAR(10)))")
        val ast = parseResult.statements[0]
        val planner = PartiQLPlanner.builder().build()
        val result = planner.plan(ast, session)
        val query = result.plan.action as Action.Query
        val actualType = query.rex.type.pType
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(10, actualType.length)
    }

    @Test
    fun `lower preserves CLOB type`() {
        val session = Session.builder()
            .catalog("default")
            .catalogs(
                TestCatalog.builder()
                    .name("default")
                    .build()
            )
            .build()
        val parseResult = PartiQLParser.standard().parse("LOWER(CAST('hello' AS CLOB))")
        val ast = parseResult.statements[0]
        val planner = PartiQLPlanner.builder().build()
        val result = planner.plan(ast, session)
        val query = result.plan.action as Action.Query
        val actualType = query.rex.type.pType
        assertEquals(PType.CLOB, actualType.code())
    }
}
