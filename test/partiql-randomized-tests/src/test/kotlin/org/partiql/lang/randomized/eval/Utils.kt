package org.partiql.lang.randomized.eval

import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumUtils
import org.partiql.value.PartiQLValue
import kotlin.test.assertEquals

fun runEvaluatorTestCase(
    query: String,
    expectedResult: String
) {
    val expected = execute(expectedResult)
    val result = execute(query)
    assertEquals(expected, result)
}

private fun execute(query: String): PartiQLValue {
    val parser = PartiQLParser.builder().build()
    val planner = PartiQLPlanner.builder().build()
    val catalog = object : Catalog {
        override fun getName(): String = "default"
    }
    val session = Session.builder().catalog("default").catalogs(catalog).build()
    val engine = PartiQLCompiler.builder().build()

    // Execute
    val stmt = parser.parse(query)
    if (stmt.statements.size != 1) error("Expected exactly one statement, got ${stmt.statements.size}")
    val plan = planner.plan(stmt.statements[0], session)
    TODO("Plan returns the sprout-generated plan, but this needs the v1 plan.")
    // val compiled = engine.prepare(plan.plan, PartiQLEngine.Mode.STRICT, session)
    // return (compiled.execute(session) as PartiQLResult.Value).value
}

fun assertExpression(query: String, value: () -> Datum) {
    val expected = DatumUtils.toPartiQLValue(value.invoke()) // TODO: Make the PartiQL Engine return a Datum, not PartiQL Value
    val result = execute(query)
    assertEquals(expected, result)
}
