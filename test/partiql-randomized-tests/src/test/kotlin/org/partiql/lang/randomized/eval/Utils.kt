package org.partiql.lang.randomized.eval

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLStatement
import org.partiql.eval.value.Datum
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Session
import org.partiql.spi.connector.Connector
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
fun runEvaluatorTestCase(
    query: String,
    expectedResult: String
) {
    val expected = execute(expectedResult)
    val result = execute(query)
    assertEquals(expected, result)
}

@OptIn(PartiQLValueExperimental::class)
private fun execute(query: String): PartiQLValue {
    val parser = PartiQLParser.builder().build()
    val planner = PartiQLPlanner.builder().build()
    val catalog = object : Catalog {
        override fun getName(): String = "default"
    }
    val plannerSession = Session.builder().catalog("default").catalogs(catalog).build()
    val engine = PartiQLEngine.builder().build()
    val connector = object : Connector {
        override fun getCatalog(): Catalog = catalog
    }
    val engineSession = PartiQLEngine.Session(
        mapOf("default" to connector)
    )

    // Execute
    val stmt = parser.parse(query)
    val plan = planner.plan(stmt.root, plannerSession)
    val compiled = engine.prepare(plan.plan, engineSession) as PartiQLStatement.Query
    return compiled.execute()
}

@OptIn(PartiQLValueExperimental::class)
fun assertExpression(query: String, value: () -> Datum) {
    val expected = value.invoke().toPartiQLValue() // TODO: Make the PartiQL Engine return a Datum, not PartiQL Value
    val result = execute(query)
    assertEquals(expected, result)
}
