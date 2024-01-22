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
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.boolValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.io.PartiQLValueIonWriterBuilder
import org.partiql.value.listValue
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
                    SELECT t
                    EXCLUDE t.a.b
                    FROM <<
                        {'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'}
                    >> AS t
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "t" to structValue(
                            "a" to structValue<PartiQLValue>(
                                // field `b` excluded
                            ),
                            "foo" to stringValue("bar"),
                            "foo2" to stringValue("bar2")
                        )
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT *
                    EXCLUDE
                        t.a.b.c[*].field_x
                    FROM [{
                        'a': {
                            'b': {
                                'c': [
                                    {                    -- c[0]; field_x to be removed
                                        'field_x': 0, 
                                        'field_y': 0
                                    },
                                    {                    -- c[1]; field_x to be removed
                                        'field_x': 1,
                                        'field_y': 1
                                    },
                                    {                    -- c[2]; field_x to be removed
                                        'field_x': 2,
                                        'field_y': 2
                                    }
                                ]
                            }
                        }
                    }] AS t
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "a" to structValue(
                            "b" to structValue(
                                "c" to listValue(
                                    structValue(
                                        "field_y" to int32Value(0)
                                    ),
                                    structValue(
                                        "field_y" to int32Value(1)
                                    ),
                                    structValue(
                                        "field_y" to int32Value(2)
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': 10, 'b': 1}, {'a': 1, 'b': 2}>> AS t ORDER BY t.a;",
                expected = listValue(
                    structValue("a" to int32Value(1), "b" to int32Value(2)),
                    structValue("a" to int32Value(10), "b" to int32Value(1))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': 10, 'b': 1}, {'a': 1, 'b': 2}>> AS t ORDER BY t.a DESC;",
                expected = listValue(
                    structValue("a" to int32Value(10), "b" to int32Value(1)),
                    structValue("a" to int32Value(1), "b" to int32Value(2))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a NULLS LAST;",
                expected = listValue(
                    structValue("a" to int32Value(1), "b" to int32Value(2)),
                    structValue("a" to int32Value(3), "b" to int32Value(4)),
                    structValue("a" to nullValue(), "b" to int32Value(1))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a NULLS FIRST;",
                expected = listValue(
                    structValue("a" to nullValue(), "b" to int32Value(1)),
                    structValue("a" to int32Value(1), "b" to int32Value(2)),
                    structValue("a" to int32Value(3), "b" to int32Value(4))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a DESC NULLS LAST;",
                expected = listValue(
                    structValue("a" to int32Value(3), "b" to int32Value(4)),
                    structValue("a" to int32Value(1), "b" to int32Value(2)),
                    structValue("a" to nullValue(), "b" to int32Value(1))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a DESC NULLS FIRST;",
                expected = listValue(
                    structValue("a" to nullValue(), "b" to int32Value(1)),
                    structValue("a" to int32Value(3), "b" to int32Value(4)),
                    structValue("a" to int32Value(1), "b" to int32Value(2))
                )
            ),
            SuccessTestCase( // use multiple sort specs
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 1, 'b': 4}>> AS t ORDER BY t.a DESC NULLS FIRST, t.b DESC;",
                expected = listValue(
                    structValue("a" to nullValue(), "b" to int32Value(1)),
                    structValue("a" to int32Value(1), "b" to int32Value(4)),
                    structValue("a" to int32Value(1), "b" to int32Value(2))
                )
            ),
        )
    }
    public class SuccessTestCase @OptIn(PartiQLValueExperimental::class) constructor(
        val input: String,
        val expected: PartiQLValue
    ) {

        private val engine = PartiQLEngine.default()
        private val planner = PartiQLPlannerBuilder().build()
        private val parser = PartiQLParser.default()

        @OptIn(PartiQLValueExperimental::class)
        internal fun assert() {
            val statement = parser.parse(input).root
            val session = PartiQLPlanner.Session("q", "u")
            val plan = planner.plan(statement, session)
            val prepared = engine.prepare(plan.plan)
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

    @Disabled("This is disabled because FN EQUALS is not yet implemented.")
    @Test
    fun testCaseLiteral02() = SuccessTestCase(
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
    ).assert()

    @Disabled("This is disabled because FN EQUALS is not yet implemented.")
    @Test
    fun testCaseLiteral03() = SuccessTestCase(
        input = """
            CASE (1)
                WHEN NULL THEN 'isNull'
                WHEN MISSING THEN 'isMissing'
                WHEN 2 THEN 'isTwo'
            END
            ;
        """.trimIndent(),
        expected = nullValue()
    ).assert()
}
