package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.eval.Mode
import org.partiql.eval.PartiQLVM
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.value.Datum

class ExecutionPlanValidationTest {

    private val parser = PartiQLParser.standard()
    private val compiler = PartiQLCompiler.standard()
    private val vm = PartiQLVM.standard()

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

    // --- compile() validation ---

    @Test
    fun compileRejectsNonRefPlan() {
        val planner = PartiQLPlanner.standard() // no useRefs — produces embedded Table
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        assertThrows<PRuntimeException> {
            compiler.compile(plan)
        }
    }

    @Test
    fun compileAcceptsRefPlan() {
        val planner = PartiQLPlanner.builder().useRefs().build()
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        assertDoesNotThrow {
            compiler.compile(plan)
        }
    }

    @Test
    fun compileAcceptsScalarOnlyPlan() {
        val planner = PartiQLPlanner.builder().useRefs().build()
        val ast = parser.parse("1 + 2").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        assertDoesNotThrow {
            compiler.compile(plan)
        }
    }

    // --- PartiQLVM.execute() validation ---

    @Test
    fun vmExecuteErrorsOnNonRefTable() {
        val planner = PartiQLPlanner.standard() // produces embedded Table
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        // compile() should reject this
        assertThrows<PRuntimeException> {
            compiler.compile(plan)
        }
    }

    @Test
    fun vmExecuteSucceedsWithRefPlan() {
        val session = buildSession()
        val planner = PartiQLPlanner.builder().useRefs().build()
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val result = planner.plan(ast, session)
        val execPlan = compiler.compile(result.plan)
        val catalogs = buildExecutionCatalogs(result.symbols, session)
        assertDoesNotThrow {
            vm.execute(execPlan, Mode.PERMISSIVE(), catalogs)
        }
    }

    // --- prepare(plan, mode) legacy path ---

    @Test
    fun legacyPrepareAcceptsNonRefPlan() {
        val planner = PartiQLPlanner.standard() // produces embedded Table
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        // Legacy prepare should NOT error — it handles embedded tables internally
        @Suppress("DEPRECATION")
        val stmt = compiler.prepare(plan, Mode.PERMISSIVE())
        assertDoesNotThrow {
            stmt.execute()
        }
    }

    @Test
    fun legacyPrepareDoesNotErrorOnCompilationWithRefPlan() {
        val session = buildSession()
        val planner = PartiQLPlanner.builder().useRefs().build()
        val ast = parser.parse("1 + 2").statements[0] // scalar, no table access
        val plan = planner.plan(ast, session).plan
        @Suppress("DEPRECATION")
        val stmt = compiler.prepare(plan, Mode.PERMISSIVE())
        assertDoesNotThrow {
            stmt.execute()
        }
    }

    // --- prepare(plan, mode, catalogs) new path ---

    @Test
    fun newPrepareRejectsNonRefPlan() {
        val planner = PartiQLPlanner.standard() // produces embedded Table
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val plan = planner.plan(ast, buildSession()).plan
        assertThrows<PRuntimeException> {
            compiler.prepare(plan, Mode.PERMISSIVE(), arrayOf<ExecutionCatalog>())
        }
    }

    @Test
    fun newPrepareSucceedsWithRefPlan() {
        val session = buildSession()
        val planner = PartiQLPlanner.builder().useRefs().build()
        val ast = parser.parse("SELECT * FROM t").statements[0]
        val result = planner.plan(ast, session)
        val catalogs = buildExecutionCatalogs(result.symbols, session)
        val stmt = compiler.prepare(result.plan, Mode.PERMISSIVE(), catalogs)
        assertDoesNotThrow {
            stmt.execute()
        }
    }
}
