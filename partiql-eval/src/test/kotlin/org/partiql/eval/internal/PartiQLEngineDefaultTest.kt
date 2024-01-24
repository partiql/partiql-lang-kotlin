package org.partiql.eval.internal

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.plugin.PartiQLPlugin
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
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
@OptIn(PartiQLValueExperimental::class)
class PartiQLEngineDefaultTest {

    @ParameterizedTest
    @MethodSource("sanityTestsCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun sanityTests(tc: SuccessTestCase) = tc.assert()

    companion object {

        @JvmStatic
        fun sanityTestsCases() = listOf(
            SuccessTestCase(
                input = "SELECT VALUE 1 FROM <<0, 1>>;",
                expected = bagValue(int32Value(1), int32Value(1))
            ),
            SuccessTestCase(
                input = "SELECT VALUE t FROM <<10, 20, 30>> AS t;",
                expected = bagValue(int32Value(10), int32Value(20), int32Value(30))
            ),
            SuccessTestCase(
                input = "SELECT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t;",
                expected = bagValue(boolValue(true), boolValue(true))
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t, << { 'b': 2 } >> s;",
                expected = bagValue(structValue("a" to int32Value(1), "b" to int32Value(2)))
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t LEFT JOIN << { 'b': 2 } >> s ON false;",
                expected = bagValue(structValue("a" to int32Value(1), "b" to nullValue()))
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t FULL OUTER JOIN << { 'b': 2 } >> s ON false;",
                expected = bagValue(
                    structValue(
                        "a" to nullValue(),
                        "b" to int32Value(2)
                    ),
                    structValue(
                        "a" to int32Value(1),
                        "b" to nullValue()
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        { 'b': TRUE },
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = structValue(
                    "a" to int32Value(1),
                    "b" to boolValue(true),
                    "c" to stringValue("hello")
                )
            ),
            SuccessTestCase(
                input = """
                    CASE
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN FALSE THEN 'isFalse'
                        WHEN TRUE THEN 'isTrue'
                    END
                    ;
                """.trimIndent(),
                expected = stringValue("isTrue")
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t FULL OUTER JOIN << { 'b': 2 } >> s ON TRUE;",
                expected = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to int32Value(2)
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        NULL,
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = structValue<PartiQLValue>(null)
            ),
            SuccessTestCase(
                input = """
                    CASE
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN FALSE THEN 'isFalse'
                    END
                    ;
                """.trimIndent(),
                expected = nullValue()
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        5,
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = missingValue()
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1, 'b': FALSE },
                        { 'b': TRUE },
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = structValue(
                    "a" to int32Value(1),
                    "b" to boolValue(false),
                    "b" to boolValue(true),
                    "c" to stringValue("hello")
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT * FROM
                    <<
                        { 'a': 1, 'b': FALSE }
                    >> AS t,
                    <<
                        { 'b': TRUE }
                    >> AS s
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to boolValue(false),
                        "b" to boolValue(true)
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE {
                        'a': 1,
                        'b': NULL,
                        t.c : t.d
                    }
                    FROM <<
                        { 'c': 'hello', 'd': 'world' }
                    >> AS t
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to nullValue(),
                        "hello" to stringValue("world")
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT v, i FROM << 'a', 'b', 'c' >> AS v AT i",
                expected = bagValue(
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
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t FROM <<true, false, true, false, false, false>> AS t;",
                expected = bagValue(boolValue(true), boolValue(false))
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t = TRUE;",
                expected = bagValue(boolValue(true))
            ),
            SuccessTestCase(
                input = "100 + 50;",
                expected = int32Value(150)
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t * 100 FROM <<0, 1, 2, 3>> AS t;",
                expected = bagValue(int32Value(0), int32Value(100), int32Value(200), int32Value(300))
            ),
            SuccessTestCase(
                input = """
                    PIVOT x.v AT x.k FROM << 
                        { 'k': 'a', 'v': 'x' },
                        { 'k': 'b', 'v': 'y' },
                        { 'k': 'c', 'v': 'z' }
                    >> AS x
                """.trimIndent(),
                expected = structValue(
                    "a" to stringValue("x"),
                    "b" to stringValue("y"),
                    "c" to stringValue("z"),
                )
            ),
            SuccessTestCase(
                input = """
                    CASE (1)
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN 2 THEN 'isTwo'
                    END
                    ;
                """.trimIndent(),
                expected = nullValue()
            ),
            SuccessTestCase(
                input = """
                    CASE (1)
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN 2 THEN 'isTwo'
                        WHEN 1 THEN 'isOne'
                    END
                    ;
                """.trimIndent(),
                expected = stringValue("isOne")
            )
        )
    }
    public class SuccessTestCase @OptIn(PartiQLValueExperimental::class) constructor(
        val input: String,
        val expected: PartiQLValue
    ) {

        @OptIn(PartiQLFunctionExperimental::class)
        private val engine = PartiQLEngine.builder().build()
        private val planner = PartiQLPlannerBuilder().build()
        private val parser = PartiQLParser.default()

        @OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
        internal fun assert() {
            val statement = parser.parse(input).root
            val session = PartiQLPlanner.Session("q", "u")
            val plan = planner.plan(statement, session)
            val functions = mapOf(
                "partiql" to PartiQLPlugin.functions
            )
            val prepared = engine.prepare(plan.plan, PartiQLEngine.Session(functions = functions))
            val result = engine.execute(prepared) as PartiQLResult.Value
            val output = result.value
            assertEquals(expected, output, comparisonString(expected, output))
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

        override fun toString(): String {
            return input
        }
    }

    @Test
    @Disabled("CASTS have not yet been implemented.")
    fun testCast1() = SuccessTestCase(
        input = "1 + 2.0",
        expected = int32Value(3),
    ).assert()

    @Test
    @Disabled("CASTS have not yet been implemented.")
    fun testCasts() = SuccessTestCase(
        input = "SELECT DISTINCT VALUE t * 100 FROM <<0, 1, 2.0, 3.0>> AS t;",
        expected = bagValue(int32Value(0), int32Value(100), int32Value(200), int32Value(300))
    ).assert()
}
