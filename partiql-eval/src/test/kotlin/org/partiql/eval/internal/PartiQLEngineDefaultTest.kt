package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.value.BagValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.boolValue
import org.partiql.value.int32Value
import kotlin.test.assertEquals

/**
 * This holds sanity tests during the development of the [PartiQLEngine.default] implementation.
 */
class PartiQLEngineDefaultTest {

    private val engine = PartiQLEngine.default()
    private val planner = PartiQLPlannerBuilder().build()
    private val parser = PartiQLParserBuilder.standard().build()

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testLiterals() {
        val statement = parser.parse("SELECT VALUE 1 FROM <<0, 1>>;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(sequenceOf(int32Value(1), int32Value(1)))
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testReference() {
        val statement = parser.parse("SELECT VALUE t FROM <<10, 20, 30>> AS t;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(sequenceOf(int32Value(10), int32Value(20), int32Value(30)))
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testFilter() {
        val statement = parser.parse("SELECT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(sequenceOf(boolValue(true), boolValue(true)))
        assertEquals(expected, output)
    }
}
