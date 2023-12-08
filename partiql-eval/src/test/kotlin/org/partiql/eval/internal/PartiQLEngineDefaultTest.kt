package org.partiql.eval.internal

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.value.BagValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.bagValue
import org.partiql.value.boolValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.nullValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import kotlin.test.assertEquals

/**
 * This holds sanity tests during the development of the [PartiQLEngine.default] implementation.
 */
@OptIn(PartiQLValueExperimental::class)
class PartiQLEngineDefaultTest {

    private val engine = PartiQLEngine.default()
    private val planner = PartiQLPlannerBuilder().build()
    private val parser = PartiQLParserBuilder.standard().build()

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

    @Test
    fun testFilter() {
        val statement =
            parser.parse("SELECT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(sequenceOf(boolValue(true), boolValue(true)))
        assertEquals(expected, output)
    }

    @Test
    fun testJoinInner() {
        val statement = parser.parse("SELECT a, b FROM << { 'a': 1 } >> t, << { 'b': 2 } >> s;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(
            sequenceOf(
                structValue(
                    "a" to int32Value(1),
                    "b" to int32Value(2),
                )
            )
        )
        assertEquals(expected, output)
    }

    @Test
    fun testJoinLeft() {
        val statement = parser.parse("SELECT a, b FROM << { 'a': 1 } >> t LEFT JOIN << { 'b': 2 } >> s ON false;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(
            sequenceOf(
                structValue(
                    "a" to int32Value(1),
                    "b" to nullValue(),
                )
            )
        )
        assertEquals(expected, output)
    }

    @Test
    @Disabled
    fun testScanIndexed() {
        val statement = parser.parse("SELECT v, i FROM << 'a', 'b', 'c' >> AS v AT i").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(
            sequenceOf(
                structValue(
                    "a" to int64Value(0),
                    "b" to int64Value(1),
                    "c" to int64Value(2),
                )
            )
        )
        assertEquals(expected, output)
    }

    @Test
    fun testPivot() {
        val statement = parser.parse(
            """
           PIVOT x.v AT x.k FROM << 
                { 'k': 'a', 'v': 'x' },
                { 'k': 'b', 'v': 'y' },
                { 'k': 'c', 'v': 'z' }
           >> AS x
            """.trimIndent()
        ).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as StructValue<*>

        val expected = structValue(
            "a" to stringValue("x"),
            "b" to stringValue("y"),
            "c" to stringValue("z"),
        )
        assertEquals(expected, output)
    }
}
