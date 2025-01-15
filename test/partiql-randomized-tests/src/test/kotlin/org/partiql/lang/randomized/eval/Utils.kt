package org.partiql.lang.randomized.eval

import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.value.Datum

fun runEvaluatorTestCaseSuccess(
    query: String,
    expectedResult: String
) {
    val expected = execute(expectedResult)
    val result = execute(query)
    val comparison = Datum.comparator().compare(expected, result)
    assert(comparison == 0) { "Expected $expected, got $result" }
}

fun runEvaluatorTestCaseFailure(
    query: String,
) {
    var thrown: Throwable? = null
    val actual: Datum = try {
        execute(query)
    } catch (t: Throwable) {
        thrown = t
        Datum.nullValue()
    }
    if (thrown == null) {
        val message = buildString {
            appendLine("Expected error to be thrown but none was thrown.")
            appendLine("Actual Result: $actual")
        }
        error(message)
    }
}

private fun execute(query: String): Datum {
    val parser = PartiQLParser.standard()
    val planner = PartiQLPlanner.standard()
    val compiler = PartiQLCompiler.standard()

    val catalog = object : Catalog {
        override fun getName(): String = "default"
    }
    val session = Session.builder().catalog("default").catalogs(catalog).build()

    // Execute
    val stmt = parser.parse(query)
    if (stmt.statements.size != 1) error("Expected exactly one statement, got ${stmt.statements.size}")
    val plan = planner.plan(stmt.statements[0], session).plan
    val statement = compiler.prepare(plan, Mode.STRICT())
    return statement.execute()
}

fun assertExpression(query: String, value: () -> Datum) {
    val expected = value.invoke()
    val result = execute(query)
    val comparison = Datum.comparator().compare(expected, result)
    assert(comparison == 0) { "Expected $expected, got $result" }
}
