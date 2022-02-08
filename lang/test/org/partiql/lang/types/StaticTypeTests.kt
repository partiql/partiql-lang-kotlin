package org.partiql.lang.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.StaticType.Companion.BLOB
import org.partiql.lang.types.StaticType.Companion.NULL
import org.partiql.lang.types.StaticType.Companion.MISSING
import org.partiql.lang.types.StaticType.Companion.INT
import org.partiql.lang.types.StaticType.Companion.INT2
import org.partiql.lang.types.StaticType.Companion.INT4
import org.partiql.lang.types.StaticType.Companion.INT8
import org.partiql.lang.types.StaticType.Companion.FLOAT
import org.partiql.lang.types.StaticType.Companion.DECIMAL
import org.partiql.lang.types.StaticType.Companion.TIMESTAMP
import org.partiql.lang.types.StaticType.Companion.BOOL
import org.partiql.lang.types.StaticType.Companion.CLOB
import org.partiql.lang.types.StaticType.Companion.STRING
import org.partiql.lang.types.StaticType.Companion.SYMBOL
import org.partiql.lang.util.ArgumentsProviderBase
import java.math.BigInteger

/**
 * At the moment, this test class only covers [StaticType.isInstance].
 *
 * This could be expanded in the future.
 */
class StaticTypeTests {

    companion object {

        class InputTypes(val sqlValue: String, val expectedTypes: List<StaticType>)

        val SCALARS: List<InputTypes> = listOf(
            // NULL & MISSING
            InputTypes("MISSING", listOf(NULL, MISSING)),
            InputTypes("NULL", listOf(NULL)),
            // BOOL
            InputTypes("true", listOf(BOOL)),
            InputTypes("false", listOf(BOOL)),
            // INT[n]
            InputTypes("1", listOf(INT, INT2, INT4, INT8)),
            InputTypes("-1", listOf(INT, INT2, INT4, INT8)),
            InputTypes("${Short.MIN_VALUE.toLong() - 1}", listOf(INT, INT4, INT8)),
            InputTypes("${Short.MAX_VALUE.toLong() + 1}", listOf(INT, INT4, INT8)),
            InputTypes("${Int.MIN_VALUE.toLong() - 1}", listOf(INT, INT8)),
            InputTypes("${Int.MAX_VALUE.toLong() + 1}", listOf(INT, INT8)),
            InputTypes("`${BigInteger.valueOf(Long.MAX_VALUE).plus(BigInteger.ONE).toLong()}`", listOf(INT, INT8)),
            InputTypes("`${BigInteger.valueOf(Long.MIN_VALUE).minus(BigInteger.ONE).toLong()}`", listOf(INT, INT8)),
            // FLOAT
            InputTypes("`1e4`", listOf(FLOAT)),
            InputTypes("1e4", listOf(DECIMAL)),
            // DECIMAL
            InputTypes("`asymbol`", listOf(SYMBOL)),
            // TIMESTAMP
            InputTypes("`2001T`", listOf(TIMESTAMP)),
            // STRING
            InputTypes("'a string'", listOf(STRING)),
            // CLOB
            InputTypes("`{{ \"Hello, world! \" }}`", listOf(CLOB)),
            // BLOB
            InputTypes("`{{ SGVsbG8sIHdvcmxkIQ== }}`", listOf(BLOB))
        )
    }

    data class TestCase(
        val sqlValue: String,
        val staticType: StaticType,
        val expectedIsInstanceResult: Boolean)

    fun eval(sql: String): ExprValue =
         CompilerPipeline.standard(ION).compile(sql).eval(EvaluationSession.standard())


    @ParameterizedTest
    @ArgumentsSource(ScalarIsInstanceArguments::class)
    fun scalarIsInstanceArgumentsTest(tc: TestCase) {

        val exprValue = assertDoesNotThrow("Evaluating the value under test should not throw") {
            eval(tc.sqlValue)
        }

        assertEquals(tc.expectedIsInstanceResult, tc.staticType.isInstance(exprValue),
            "The result of StaticType.isInstance() should match the expected value for type ${tc.staticType} and \"${tc.sqlValue}\"")
    }

    class ScalarIsInstanceArguments : ArgumentsProviderBase() {
        // For the given list of values of each possible data type, compute a one test case for each of the
        // possible data types.
        override fun getParameters(): List<TestCase> {
            return SCALARS.flatMap { p ->
                StaticType.ALL_TYPES.map { TestCase(p.sqlValue, it, p.expectedTypes.contains(it)) }
            }
        }
    }


    @ParameterizedTest
    @ArgumentsSource(SequenceIsInstanceArguments::class)
    fun sequenceIsInstanceArgumentsTest(tc: TestCase) {

        val exprValue = assertDoesNotThrow("Evaluating the value under test should not throw") {
            eval(tc.sqlValue)
        }

        assertEquals(tc.expectedIsInstanceResult, tc.staticType.isInstance(exprValue),
            "The result of StaticType.isInstance() should match the expected value for type ${tc.staticType} and \"${tc.sqlValue}\"")
    }

    class SequenceIsInstanceArguments : ArgumentsProviderBase() {

        override fun getParameters(): List<TestCase> {
            val listType = ListType()
            return listOf(
                // An empty list and an empty s-expression should be a list of s-exp of any type
                StaticType.ALL_TYPES.map {
                    TestCase(
                        sqlValue = "[]",
                        staticType = ListType(it),
                        expectedIsInstanceResult = true
                    )
                },
                StaticType.ALL_TYPES.map {
                    TestCase(
                        sqlValue = "SEXP()",
                        staticType = SexpType(it),
                        expectedIsInstanceResult = true
                    )
                },

                // list with a single value
                SCALARS.flatMap { p ->
                    StaticType.ALL_TYPES.map {
                        TestCase(
                            sqlValue = "[${p.sqlValue}]",
                            staticType = ListType(it),
                            expectedIsInstanceResult = p.expectedTypes.contains(it)
                        )
                    }
                },

                // s-exp with a a single value
                SCALARS.flatMap { p ->
                    StaticType.ALL_TYPES.map {
                        TestCase(
                            sqlValue = "SEXP(${p.sqlValue})",
                            staticType = SexpType(it),
                            expectedIsInstanceResult = p.expectedTypes.contains(it)
                        )
                    }
                },

                // list with a multiple values
                SCALARS.flatMap { p1 ->
                    SCALARS.flatMap { p2 ->
                        StaticType.ALL_TYPES.map {
                            TestCase(
                                sqlValue = "[${p1.sqlValue}, ${p2.sqlValue}]",
                                staticType = ListType(it),
                                expectedIsInstanceResult = p1.expectedTypes.contains(it) && p2.expectedTypes.contains(it)
                            )
                        }
                    }
                },
                // s-exp with a multiple values
                SCALARS.flatMap { p1 ->
                    SCALARS.flatMap { p2 ->
                        StaticType.ALL_TYPES.map {
                            TestCase(
                                sqlValue = "SEXP(${p1.sqlValue}, ${p2.sqlValue})",
                                staticType = SexpType(it),
                                expectedIsInstanceResult = p1.expectedTypes.contains(it) && p2.expectedTypes.contains(it)
                            )
                        }
                    }
                }
            ).flatten()
        }
    }

    @ParameterizedTest
    @ArgumentsSource(StructIsInstanceArguments::class)
    fun structIsInstanceTest(tc: TestCase) {

        val exprValue = assertDoesNotThrow("Evaluating the value under test should not throw") {
            eval(tc.sqlValue)
        }

        assertEquals(tc.expectedIsInstanceResult, tc.staticType.isInstance(exprValue),
            "The result of StaticType.isInstance() should match the expected value for type ${tc.staticType} and \"${tc.sqlValue}\"")
    }

    class StructIsInstanceArguments : ArgumentsProviderBase() {
        // For the given list of values of each possible data type, compute a one test case for each of the
        override fun getParameters(): List<TestCase> {
            return listOf(
                // for structs (have to exclude missing since missing values are not materialized into structs).
                SCALARS.filterNot { it.sqlValue == "MISSING" }.flatMap { scalarInput1 ->
                    StaticType.ALL_TYPES.filterNot { it is MissingType }.flatMap { staticType ->
                        val closedContentStructType = StructType(mapOf("foo" to staticType), contentClosed = true)
                        val openContentStructType = StructType(mapOf("foo" to staticType), contentClosed = false)
                        val closedContentWithOptionalField = StructType(mapOf("foo" to AnyOfType(setOf(MISSING, staticType))), contentClosed = true)
                        listOf(
                            listOf(
                                // closed content with matching field
                                TestCase(
                                    sqlValue = "{'foo': ${scalarInput1.sqlValue} }",
                                    staticType = closedContentStructType,
                                    expectedIsInstanceResult = scalarInput1.expectedTypes.contains(staticType)),

                                // open content where all fields match
                                TestCase(
                                    sqlValue = "{'foo': ${scalarInput1.sqlValue}, 'openContent': 'isAllowed' }",
                                    staticType = openContentStructType,
                                    expectedIsInstanceResult = scalarInput1.expectedTypes.contains(staticType)),

                                // closed content with missing required field
                                TestCase(
                                    sqlValue = "{ }",
                                    staticType = closedContentStructType,
                                    expectedIsInstanceResult = false),

                                // closed content with non-matching field
                                TestCase(
                                    sqlValue = "{'bar': ${scalarInput1.sqlValue} }",
                                    staticType = closedContentStructType,
                                    expectedIsInstanceResult = false),

                                // closed content with optional field that is present
                                TestCase(
                                    sqlValue = "{'foo': ${scalarInput1.sqlValue} }",
                                    staticType = closedContentWithOptionalField,
                                    expectedIsInstanceResult = scalarInput1.expectedTypes.contains(staticType)),

                                // closed content with optional field that is not present
                                TestCase(
                                    sqlValue = "{ }",
                                    staticType = closedContentWithOptionalField,
                                    expectedIsInstanceResult = true),

                                // open content with missing required field
                                TestCase(
                                    sqlValue = "{ 'openContent': 'isAllowed' }",
                                    staticType = openContentStructType,
                                    expectedIsInstanceResult = false),

                                // open content with a non-matching field
                                TestCase(
                                    sqlValue = "{'bar': ${scalarInput1.sqlValue}, 'openContent': 'isAllowed' }",
                                    staticType = openContentStructType,
                                    expectedIsInstanceResult = false),

                                // open content with an non-matching field
                                TestCase(
                                    sqlValue = "{'bar': ${scalarInput1.sqlValue}, 'openContent': 'isNotAllowed' }",
                                    staticType = openContentStructType,
                                    expectedIsInstanceResult = false),

                                // duplicate struct fields with values of the same type
                                TestCase(
                                    sqlValue = "{'foo': ${scalarInput1.sqlValue}, 'foo': ${scalarInput1.sqlValue} }",
                                    staticType = closedContentStructType,
                                    expectedIsInstanceResult = scalarInput1.expectedTypes.contains(staticType))),
                        // Duplicate struct fields with values of different types.
                        // We generate one test case for every scalar value with every other scalar value and
                        // every type.
                        SCALARS.filterNot { it.sqlValue == "MISSING" }.map { scalarInput2 ->
                            TestCase(
                                sqlValue = "{'foo': ${scalarInput1.sqlValue}, 'foo': ${scalarInput2.sqlValue} }",
                                staticType = closedContentStructType,
                                // We expect the result to be `TRUE` only if the values of the foo and bar fields
                                // match the static type.
                                expectedIsInstanceResult = scalarInput1.expectedTypes.contains(staticType) && scalarInput2.expectedTypes.contains(staticType))

                        }).flatten()
                    }
                }
            ).flatten()
        }
    }
}
