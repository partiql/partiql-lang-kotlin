package org.partiql.eval.internal

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.value.BagValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.bagValue
import org.partiql.value.boolValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.io.PartiQLValueIonWriterBuilder
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

/**
 * This holds sanity tests during the development of the [PartiQLEngine.default] implementation.
 */
class PartiQLEngineDefaultTest {

    private val engine = PartiQLEngine.default()
    private val planner = PartiQLPlannerBuilder().build()
    private val parser = PartiQLParser.default()

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testLiterals() {
        val statement = parser.parse("SELECT VALUE 1 FROM <<0, 1>>;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(int32Value(1), int32Value(1))
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

        val expected = bagValue(int32Value(10), int32Value(20), int32Value(30))
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testFilter() {
        val statement =
            parser.parse("SELECT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(boolValue(true), boolValue(true))
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testJoinInner() {
        val statement = parser.parse("SELECT t.a, s.b FROM << { 'a': 1 } >> t, << { 'b': 2 } >> s;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(structValue("a" to int32Value(1), "b" to int32Value(2)))
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testJoinLeft() {
        val statement = parser.parse("SELECT t.a, s.b FROM << { 'a': 1 } >> t LEFT JOIN << { 'b': 2 } >> s ON false;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(structValue("a" to int32Value(1), "b" to nullValue()))
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testJoinOuterFull() {
        val statement =
            parser.parse("SELECT t.a, s.b FROM << { 'a': 1 } >> t FULL OUTER JOIN << { 'b': 2 } >> s ON false;").root

        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)

        val result = engine.execute(prepared)
        if (result is PartiQLResult.Error) {
            throw result.cause
        }
        result as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(
            structValue(
                "a" to nullValue(),
                "b" to int32Value(2)
            ),
            structValue(
                "a" to int32Value(1),
                "b" to nullValue()
            ),
        )
        assertEquals(expected, output, comparisonString(expected, output))
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testTupleUnion() {
        val source = """
            TUPLEUNION(
                { 'a': 1 },
                { 'b': TRUE },
                { 'c': 'hello' }
            );
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value

        val expected = structValue(
            "a" to int32Value(1),
            "b" to boolValue(true),
            "c" to stringValue("hello")
        )
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testCaseLiteral00() {
        val source = """
            CASE
                WHEN NULL THEN 'isNull'
                WHEN MISSING THEN 'isMissing'
                WHEN FALSE THEN 'isFalse'
                WHEN TRUE THEN 'isTrue'
            END
            ;
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value

        val expected = stringValue("isTrue")
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testJoinOuterFullOnTrue() {
        val statement =
            parser.parse("SELECT t.a, s.b FROM << { 'a': 1 } >> t FULL OUTER JOIN << { 'b': 2 } >> s ON TRUE;").root

        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)

        val result = engine.execute(prepared)
        if (result is PartiQLResult.Error) {
            throw result.cause
        }
        result as PartiQLResult.Value
        val output = result.value as BagValue<*>

        val expected = bagValue(
            structValue(
                "a" to int32Value(1),
                "b" to int32Value(2)
            ),
        )
        assertEquals(expected, output, comparisonString(expected, output))
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testTupleUnionNullInput() {
        val source = """
            TUPLEUNION(
                { 'a': 1 },
                NULL,
                { 'c': 'hello' }
            );
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value

        val expected = structValue<PartiQLValue>(null)

        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testCaseLiteral01() {
        val source = """
            CASE
                WHEN NULL THEN 'isNull'
                WHEN MISSING THEN 'isMissing'
                WHEN FALSE THEN 'isFalse'
            END
            ;
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)

        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value

        val expected = nullValue()
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun comparisonString(expected: PartiQLValue, actual: PartiQLValue): String {
        val expectedBuffer = ByteArrayOutputStream()
        val expectedWriter = PartiQLValueIonWriterBuilder.standardIonTextBuilder().build(expectedBuffer)
        expectedWriter.append(expected)
        return buildString {
            appendLine("Expected : $expectedBuffer")
            expectedBuffer.reset()
            expectedWriter.append(actual)
            appendLine("Actual   : $expectedBuffer")
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testTupleUnionBadInput() {
        val source = """
            TUPLEUNION(
                { 'a': 1 },
                5,
                { 'c': 'hello' }
            );
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value
        val expected = missingValue()
        assertEquals(expected, output)
    }

    @Disabled("This is disabled because FN EQUALS is not yet implemented.")
    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testCaseLiteral02() {
        val source = """
            CASE (1)
                WHEN NULL THEN 'isNull'
                WHEN MISSING THEN 'isMissing'
                WHEN 2 THEN 'isTwo'
                WHEN 1 THEN 'isOne'
            END
            ;
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value

        val expected = stringValue("isOne")
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testTupleUnionDuplicates() {
        val source = """
            TUPLEUNION(
                { 'a': 1, 'b': FALSE },
                { 'b': TRUE },
                { 'c': 'hello' }
            );
            
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value

        val expected = structValue(
            "a" to int32Value(1),
            "b" to boolValue(false),
            "b" to boolValue(true),
            "c" to stringValue("hello")
        )
        assertEquals(expected, output)
    }

    @Disabled("This is disabled because FN EQUALS is not yet implemented.")
    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testCaseLiteral03() {
        val source = """
            CASE (1)
                WHEN NULL THEN 'isNull'
                WHEN MISSING THEN 'isMissing'
                WHEN 2 THEN 'isTwo'
            END
            ;
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value
        val expected = nullValue()
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testSelectStarTupleUnion() {
        // As SELECT * gets converted to TUPLEUNION, this is a sanity check
        val source = """
            SELECT * FROM
            <<
                { 'a': 1, 'b': FALSE }
            >> AS t,
            <<
                { 'b': TRUE }
            >> AS s
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value

        val expected = bagValue(
            structValue(
                "a" to int32Value(1),
                "b" to boolValue(false),
                "b" to boolValue(true)
            )
        )
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testStruct() {
        val source = """
            SELECT VALUE {
                'a': 1,
                'b': NULL,
                t.c : t.d
            }
            FROM <<
                { 'c': 'hello', 'd': 'world' }
            >> AS t
        """.trimIndent()
        val statement = parser.parse(source).root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared)
        if (result is PartiQLResult.Error) {
            throw result.cause
        }
        val output = (result as PartiQLResult.Value).value

        val expected: PartiQLValue = bagValue(
            structValue(
                "a" to int32Value(1),
                "b" to nullValue(),
                "hello" to stringValue("world")
            )
        )
        assertEquals(expected, output)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testScanIndexed() {
        val statement = parser.parse("SELECT v, i FROM << 'a', 'b', 'c' >> AS v AT i").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session)

        val prepared = engine.prepare(plan.plan)
        val result = engine.execute(prepared) as PartiQLResult.Value
        val output = result.value

        val expected = bagValue(
            structValue(
                "v" to stringValue("a"),
                "i" to int64Value(0),
            ),
            structValue(
                "v" to stringValue("b"),
                "i" to int64Value(1),
            ),
            structValue(
                "v" to stringValue("c"),
                "i" to int64Value(2),
            ),
        )
        assertEquals(expected, output, comparisonString(expected, output))
    }

    @OptIn(PartiQLValueExperimental::class)
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
        val result = engine.execute(prepared)
        if (result is PartiQLResult.Error) {
            throw result.cause
        }
        result as PartiQLResult.Value
        val output = result.value as StructValue<*>

        val expected = structValue(
            "a" to stringValue("x"),
            "b" to stringValue("y"),
            "c" to stringValue("z"),
        )
        assertEquals(expected, output, comparisonString(expected, output))
    }
}
