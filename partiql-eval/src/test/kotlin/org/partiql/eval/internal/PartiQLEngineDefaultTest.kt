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
import org.partiql.value.decimalValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.io.PartiQLValueIonWriterBuilder
import org.partiql.value.listValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * This holds sanity tests during the development of the [PartiQLEngine.default] implementation.
 */
@OptIn(PartiQLValueExperimental::class)
class PartiQLEngineDefaultTest {

    @ParameterizedTest
    @MethodSource("sanityTestsCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun sanityTests(tc: SuccessTestCase) = tc.assert()

    @ParameterizedTest
    @MethodSource("typingModeTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun typingModeTests(tc: TypingTestCase) = tc.assert()

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
            ),
            SuccessTestCase(
                input = """
                    `null.bool` IS NULL
                """.trimIndent(),
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = """
                    1 + (SELECT t.a FROM << { 'a': 3 } >> AS t)
                """.trimIndent(),
                expected = int32Value(4)
            ),
            // SELECT * without nested coercion
            SuccessTestCase(
                input = """
                    SELECT *
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "first" to int32Value(3),
                        "second" to int32Value(5)
                    )
                )
            ),
            // SELECT list without nested coercion
            SuccessTestCase(
                input = """
                    SELECT "first", "second"
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "first" to int32Value(3),
                        "second" to int32Value(5)
                    )
                )
            ),
            // SELECT value without nested coercion
            SuccessTestCase(
                input = """
                    SELECT VALUE "first"
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = bagValue(
                    int32Value(3),
                )
            ),
            SuccessTestCase(
                input = "MISSING IS MISSING;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "MISSING IS MISSING;",
                expected = boolValue(true), // TODO: Is this right?
                mode = PartiQLEngine.Mode.STRICT
            ),
            SuccessTestCase(
                input = "SELECT VALUE t.a IS MISSING FROM << { 'b': 1 }, { 'a': 2 } >> AS t;",
                expected = bagValue(boolValue(true), boolValue(false))
            ),
            // PartiQL Specification Section 7.1.1 -- Equality
            SuccessTestCase(
                input = "5 = 'a';",
                expected = boolValue(false),
            ),
            // PartiQL Specification Section 7.1.1 -- Equality
            SuccessTestCase(
                input = "5 = 'a';",
                expected = boolValue(false), // TODO: Is this correct?
                mode = PartiQLEngine.Mode.STRICT
            ),
            // PartiQL Specification Section 8
            SuccessTestCase(
                input = "MISSING AND TRUE;",
                expected = boolValue(null),
            ),
            // PartiQL Specification Section 8
            SuccessTestCase(
                input = "MISSING AND TRUE;",
                expected = boolValue(null), // TODO: Is this right?
                mode = PartiQLEngine.Mode.STRICT
            ),
            // PartiQL Specification Section 8
            SuccessTestCase(
                input = "NULL IS MISSING;",
                expected = boolValue(false),
            ),
            // PartiQL Specification Section 8
            SuccessTestCase(
                input = "NULL IS MISSING;",
                expected = boolValue(false),
                mode = PartiQLEngine.Mode.STRICT
            ),
        )

        @JvmStatic
        fun typingModeTestCases() = listOf(
            TypingTestCase(
                name = "Expected missing value in collection",
                input = "SELECT VALUE t.a FROM << { 'a': 1 }, { 'b': 2 } >> AS t;",
                expectedPermissive = bagValue(int32Value(1), missingValue())
            ),
            TypingTestCase(
                name = "Expected missing value in tuple in collection",
                input = "SELECT t.a AS \"a\" FROM << { 'a': 1 }, { 'b': 2 } >> AS t;",
                expectedPermissive = bagValue(
                    structValue(
                        "a" to int32Value(1),
                    ),
                    structValue(),
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 4.2 -- index negative",
                input = "[1,2,3][-1];",
                expectedPermissive = missingValue()
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 4.2 -- out of bounds",
                input = "[1,2,3][3];",
                expectedPermissive = missingValue()
            ),
            TypingTestCase(
                name = "PartiQL Spec Section 5.1.1 -- Position variable on bags",
                input = "SELECT v, p FROM << 5 >> AS v AT p;",
                expectedPermissive = bagValue(
                    structValue(
                        "v" to int32Value(5)
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a scalar value",
                input = "SELECT v FROM 0 AS v;",
                expectedPermissive = bagValue(
                    structValue(
                        "v" to int32Value(0)
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a scalar value (with at)",
                input = "SELECT v, p FROM 0 AS v AT p;",
                expectedPermissive = bagValue(
                    structValue(
                        "v" to int32Value(0)
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a tuple value",
                input = "SELECT v.a AS a FROM { 'a': 1 } AS v;",
                expectedPermissive = bagValue(
                    structValue(
                        "a" to int32Value(1)
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over an absent value (missing)",
                input = "SELECT v AS v FROM MISSING AS v;",
                expectedPermissive = bagValue(structValue<PartiQLValue>())
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over an absent value (null)",
                input = "SELECT v AS v FROM NULL AS v;",
                expectedPermissive = bagValue(
                    structValue(
                        "v" to nullValue()
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing tuples",
                input = "SELECT VALUE {'a':v.a, 'b':v.b} FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to int32Value(1),
                    ),
                    structValue(
                        "a" to int32Value(2),
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing bags (1)",
                input = "SELECT VALUE v.b FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = bagValue(
                    int32Value(1),
                    missingValue()
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing bags (2)",
                input = "SELECT VALUE <<v.a, v.b>> FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = bagValue(
                    bagValue(
                        int32Value(1),
                        int32Value(1),
                    ),
                    bagValue(
                        int32Value(2),
                        missingValue()
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.2 -- Pivoting a Collection into a Variable-Width Tuple",
                input = "PIVOT t.price AT t.\"symbol\" FROM [{'symbol':25, 'price':31.52}, {'symbol':'amzn', 'price':840.05}] AS t;",
                expectedPermissive = structValue(
                    "amzn" to decimalValue(BigDecimal.valueOf(840.05))
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (1)",
                input = "SELECT VALUE 5 + v FROM <<1, MISSING>> AS v;",
                expectedPermissive = bagValue(int32Value(6), missingValue())
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (3)",
                input = "SELECT VALUE NOT v FROM << false, {'a':1} >> AS v;",
                expectedPermissive = bagValue(boolValue(true), missingValue())
            ),

            TypingTestCase(
                name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (2)",
                input = "SELECT VALUE 5 > v FROM <<1, 'a'>> AS v;",
                expectedPermissive = bagValue(boolValue(true), missingValue())
            ),
        )
    }

    public class SuccessTestCase @OptIn(PartiQLValueExperimental::class) constructor(
        val input: String,
        val expected: PartiQLValue,
        val mode: PartiQLEngine.Mode = PartiQLEngine.Mode.PERMISSIVE
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
            val prepared = engine.prepare(plan.plan, PartiQLEngine.Session(functions = functions, mode = mode))
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

    public class TypingTestCase @OptIn(PartiQLValueExperimental::class) constructor(
        val name: String,
        val input: String,
        val expectedPermissive: PartiQLValue
    ) {

        @OptIn(PartiQLFunctionExperimental::class)
        private val engine = PartiQLEngine.builder().build()
        private val planner = PartiQLPlannerBuilder().build()
        private val parser = PartiQLParser.default()

        @OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
        internal fun assert() {
            val permissiveResult = run(mode = PartiQLEngine.Mode.PERMISSIVE)
            assertEquals(expectedPermissive, permissiveResult, comparisonString(expectedPermissive, permissiveResult))
            var error: Throwable? = null
            try {
                run(mode = PartiQLEngine.Mode.STRICT)
            } catch (e: Throwable) {
                error = e
            }
            assertNotNull(error)
        }

        @OptIn(PartiQLFunctionExperimental::class)
        private fun run(mode: PartiQLEngine.Mode): PartiQLValue {
            val statement = parser.parse(input).root
            val session = PartiQLPlanner.Session("q", "u")
            val plan = planner.plan(statement, session)
            val functions = mapOf(
                "partiql" to PartiQLPlugin.functions
            )
            val prepared = engine.prepare(plan.plan, PartiQLEngine.Session(functions = functions, mode = mode))
            when (val result = engine.execute(prepared)) {
                is PartiQLResult.Value -> return result.value
                is PartiQLResult.Error -> throw result.cause
            }
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
            return "$name -- $input"
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

    @Test
    @Disabled("We need to support section 5.1")
    fun testTypingOfPositionVariable() = TypingTestCase(
        name = "PartiQL Spec Section 5.1.1 -- Position variable on bags",
        input = "SELECT v, p FROM << 5 >> AS v AT p;",
        expectedPermissive = bagValue(
            structValue(
                "v" to int32Value(5)
            )
        )
    ).assert()

    @Test
    @Disabled("Subqueries aren't supported yet.")
    fun test() = TypingTestCase(
        name = "PartiQL Specification Section 9.1",
        input = """
            SELECT o.name AS orderName,
                (SELECT c.name FROM << { 'name': 'John', 'id': 1 }, { 'name': 'Alan', 'id': 1 } >> c WHERE c.id=o.custId) AS customerName
            FROM << { 'name': 'apples', 'custId': 1 } >> o
        """.trimIndent(),
        expectedPermissive = bagValue(
            structValue(
                "orderName" to stringValue("apples")
            )
        )
    ).assert()

    @Test
    @Disabled("This is just a placeholder. We should add support for this. Grouping is not yet supported.")
    fun test3() =
        TypingTestCase(
            name = "PartiQL Specification Section 11.1",
            input = """
                    PLACEHOLDER FOR THE EXAMPLE IN THE RELEVANT SECTION. GROUPING NOT YET SUPPORTED.
            """.trimIndent(),
            expectedPermissive = missingValue()
        ).assert()

    @Test
    @Disabled("The planner fails this, though it should pass for permissive mode.")
    fun test5() =
        TypingTestCase(
            name = "PartiQL Specification Section 5.2.1 -- Mistyping Cases",
            input = "SELECT v, n FROM UNPIVOT 1 AS v AT n;",
            expectedPermissive = bagValue(
                structValue(
                    "v" to int32Value(1),
                    "n" to stringValue("_1")
                )
            )
        ).assert()

    @Test
    @Disabled("We don't yet support arrays.")
    fun test7() =
        TypingTestCase(
            name = "PartiQL Specification Section 6.1.4 -- when constructing arrays",
            input = "SELECT VALUE [v.a, v.b] FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
            expectedPermissive = bagValue(
                listValue(
                    int32Value(1),
                    int32Value(1),
                ),
                listValue(
                    int32Value(2),
                    missingValue()
                )
            )
        ).assert()

    @Test
    @Disabled("There is a bug in the planner which makes this always return missing.")
    fun test8() =
        TypingTestCase(
            name = "PartiQL Specification Section 4.2 -- non integer index",
            input = "SELECT VALUE [1,2,3][v] FROM <<1, 1.0>> AS v;",
            expectedPermissive = bagValue(int32Value(2), missingValue())
        ).assert()

    @Test
    @Disabled("CASTs aren't supported yet.")
    fun test9() =
        TypingTestCase(
            name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 27",
            input = "SELECT VALUE {'a':3*v.a, 'b':3*(CAST (v.b AS INTEGER))} FROM [{'a':1, 'b':'1'}, {'a':2}] v;",
            expectedPermissive = bagValue(
                structValue(
                    "a" to int32Value(3),
                    "b" to int32Value(3),
                ),
                structValue(
                    "a" to int32Value(6),
                ),
            )
        ).assert()

    @Test
    @Disabled("Arrays aren't supported yet.")
    fun test10() =
        SuccessTestCase(
            input = "SELECT v, i FROM [ 'a', 'b', 'c' ] AS v AT i",
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
        ).assert()

    @Test
    @Disabled("Support for ORDER BY needs to be added for this to pass.")
    // PartiQL Specification says that SQL's SELECT is coerced, but SELECT VALUE is not.
    fun selectValueNoCoercion() =
        SuccessTestCase(
            input = """
                (4, 5) < (SELECT VALUE t.a FROM << { 'a': 3 }, { 'a': 4 } >> AS t ORDER BY t.a)
            """.trimIndent(),
            expected = boolValue(false)
        ).assert()

    @Test
    @Disabled("This is appropriately coerced, but this test is failing because LT currently doesn't support LISTS.")
    fun rowCoercion() =
        SuccessTestCase(
            input = """
                (4, 5) < (SELECT t.a, t.a FROM << { 'a': 3 } >> AS t)
            """.trimIndent(),
            expected = boolValue(false)
        ).assert()
}
