package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.value.Datum

class ExecutionPlanValidationTest {

    private val parser = PartiQLParser.standard()
    private val compiler = PartiQLCompiler.standard()

    private fun buildSession(): Session {
        val catalog = Catalog.builder()
            .name("test")
            .define(Table.standard(Name.of("t"), Datum.bagVararg(Datum.integer(1))))
            .build()
        return Session.builder()
            .catalog("test")
            .catalogs(catalog)
            .build()
    }

    @Test
    fun rejectsNonRefPlan() {
        val planner = PartiQLPlanner.standard() // no useRefs
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        assertThrows<PRuntimeException> {
            compiler.compile(plan)
        }
    }

    @Test
    fun acceptsRefPlan() {
        val planner = PartiQLPlanner.builder().useRefs().build()
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        assertDoesNotThrow {
            compiler.compile(plan)
        }
    }

    @Test
    fun acceptsScalarOnlyRefPlan() {
        val planner = PartiQLPlanner.builder().useRefs().build()
        val ast = parser.parse("1 + 2").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        assertDoesNotThrow {
            compiler.compile(plan)
        }
    }
}
