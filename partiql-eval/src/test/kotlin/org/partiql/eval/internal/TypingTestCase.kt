package org.partiql.eval.internal

import org.partiql.eval.Mode
import org.partiql.eval.PTestCase
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

    override fun run() {
        val (permissiveResult, plan) = run(mode = Mode.PERMISSIVE())
        val assertionCondition = try {
            Datum.comparator().compare(expectedPermissive, permissiveResult) == 0
        } catch (t: Throwable) {
            val str = buildString {
                appendLine("Test Name: $name")
                // TODO pretty-print V1 plans!
                appendLine(plan)
            }
            throw RuntimeException(str, t)
        }
        assert(assertionCondition) {
            comparisonString(expectedPermissive, permissiveResult, plan)
        }
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
