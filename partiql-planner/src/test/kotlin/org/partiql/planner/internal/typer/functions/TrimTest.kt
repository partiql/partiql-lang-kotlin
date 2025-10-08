package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.TestCatalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import kotlin.test.assertEquals

class TrimTest {

    @Test
    fun `trim preserves CHAR length and type`() {
        val session = Session.builder()
            .catalog("default")
            .catalogs(
                TestCatalog.builder()
                    .name("default")
                    .build()
            )
            .build()
        val parseResult = PartiQLParser.standard().parse("TRIM(CAST('  HELLO  ' AS CHAR(9)))")
        val ast = parseResult.statements[0]
        val planner = PartiQLPlanner.builder().build()
        val result = planner.plan(ast, session)
        val query = result.plan.action as Action.Query
        val actualType = query.rex.type.pType
        assertEquals(PType.CHAR, actualType.code())
        assertEquals(9, actualType.length)
    }

    @Test
    fun `trim preserves VARCHAR length and type`() {
        val session = Session.builder()
            .catalog("default")
            .catalogs(
                TestCatalog.builder()
                    .name("default")
                    .build()
            )
            .build()
        val parseResult = PartiQLParser.standard().parse("TRIM(CAST('  HELLO  ' AS VARCHAR(15)))")
        val ast = parseResult.statements[0]
        val planner = PartiQLPlanner.builder().build()
        val result = planner.plan(ast, session)
        val query = result.plan.action as Action.Query
        val actualType = query.rex.type.pType
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(15, actualType.length)
    }

    @Test
    fun `trim preserves CLOB type`() {
        val session = Session.builder()
            .catalog("default")
            .catalogs(
                TestCatalog.builder()
                    .name("default")
                    .build()
            )
            .build()
        val parseResult = PartiQLParser.standard().parse("TRIM(CAST('  HELLO  ' AS CLOB))")
        val ast = parseResult.statements[0]
        val planner = PartiQLPlanner.builder().build()
        val result = planner.plan(ast, session)
        val query = result.plan.action as Action.Query
        val actualType = query.rex.type.pType
        assertEquals(PType.CLOB, actualType.code())
    }

    @Test
    fun `trim preserves STRING type`() {
        val session = Session.builder()
            .catalog("default")
            .catalogs(
                TestCatalog.builder()
                    .name("default")
                    .build()
            )
            .build()
        val parseResult = PartiQLParser.standard().parse("TRIM('  HELLO  ')")
        val ast = parseResult.statements[0]
        val planner = PartiQLPlanner.builder().build()
        val result = planner.plan(ast, session)
        val query = result.plan.action as Action.Query
        val actualType = query.rex.type.pType
        assertEquals(PType.STRING, actualType.code())
    }
}
