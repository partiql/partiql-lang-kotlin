package org.partiql.eval.internal

import org.partiql.eval.Mode
import org.partiql.eval.PTestCase
import org.partiql.eval.PartiQLVM
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

public class TypingTestCase(
    val name: String,
    val input: String,
    val expectedPermissive: Datum,
) : PTestCase {

    private val compiler = PartiQLCompiler.standard()
    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val refPlanner = PartiQLPlanner.builder().useRefs().build()
    private val vm = PartiQLVM.standard()

    override fun run() {
        // Old path: PERMISSIVE should produce expectedPermissive
        val (permissiveResult, plan) = run(mode = Mode.PERMISSIVE())
        val assertionCondition = try {
            Datum.comparator().compare(expectedPermissive, permissiveResult) == 0
        } catch (t: Throwable) {
            val str = buildString {
                appendLine("Test Name: $name")
                appendLine(plan)
            }
            throw RuntimeException(str, t)
        }
        assert(assertionCondition) {
            comparisonString(expectedPermissive, permissiveResult, plan)
        }
        // Old path: STRICT should throw
        var error: Throwable? = null
        try {
            val (strictResult, _) = run(mode = Mode.STRICT())
            when (strictResult.type.code()) {
                PType.BAG, PType.ARRAY -> strictResult.toList()
                else -> strictResult
            }
        } catch (e: Throwable) {
            error = e
        }
        assertNotNull(error)
        // VM path: PERMISSIVE should produce same result
        val (vmPermissiveResult, vmPlan) = runVM(mode = Mode.PERMISSIVE())
        val vmCondition = try {
            Datum.comparator().compare(expectedPermissive, vmPermissiveResult) == 0
        } catch (t: Throwable) {
            val str = buildString {
                appendLine("[VM PATH] Test Name: $name")
                appendLine(vmPlan)
            }
            throw RuntimeException(str, t)
        }
        assert(vmCondition) {
            buildString {
                appendLine("[VM PATH]")
                appendLine(comparisonString(expectedPermissive, vmPermissiveResult, vmPlan))
            }
        }
        // VM path: STRICT should throw
        var vmError: Throwable? = null
        try {
            val (vmStrictResult, _) = runVM(mode = Mode.STRICT())
            when (vmStrictResult.type.code()) {
                PType.BAG, PType.ARRAY -> vmStrictResult.toList()
                else -> vmStrictResult
            }
        } catch (e: Throwable) {
            vmError = e
        }
        assertNotNull(vmError)
    }

    private fun run(mode: Mode): Pair<Datum, Plan> {
        val parseResult = parser.parse(input)
        assertEquals(1, parseResult.statements.size)
        val statement = parseResult.statements[0]
        val catalog = Catalog.builder().name("memory").build()
        val session = Session.builder()
            .catalog("memory")
            .catalogs(catalog)
            .build()
        val plan = planner.plan(statement, session).plan
        val result = compiler.prepare(plan, mode).execute()
        return result to plan
    }

    private fun runVM(mode: Mode): Pair<Datum, Plan> {
        val parseResult = parser.parse(input)
        assertEquals(1, parseResult.statements.size)
        val statement = parseResult.statements[0]
        val catalog = Catalog.builder().name("memory").build()
        val session = Session.builder()
            .catalog("memory")
            .catalogs(catalog)
            .build()
        val refResult = refPlanner.plan(statement, session)
        val execPlan = compiler.compile(refResult.plan, mode)
        val catalogs = buildExecutionCatalogs(refResult.symbols, session)
        val result = vm.execute(execPlan, catalogs)
        return result to refResult.plan
    }

    private fun comparisonString(expected: Datum, actual: Datum, plan: Plan): String {
        return buildString {
            // TODO pretty-print V1 plans!
            appendLine(plan)
            appendLine("Expected : $expected")
            appendLine("Actual   : $actual")
        }
    }

    override fun toString(): String {
        return "$name -- $input"
    }
}
