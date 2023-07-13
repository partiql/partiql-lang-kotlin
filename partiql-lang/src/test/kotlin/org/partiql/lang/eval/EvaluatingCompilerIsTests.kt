package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.util.ArgumentsProviderBase

/**
 * Constructs 8 test cases for the specified cast expression, replacing "{TYPE}" in [sql] with
 * `CHAR`, `VARCHAR` or `CHARACTER VARYING`.
 *
 * In [TypedOpBehavior.HONOR_PARAMETERS] mode, `IS CHAR(n)` or `IS VARCHAR(n)` inspect the length of the
 * left-hand value according to different rules.  The expected results are stored in [expectedIsCharHonorParamsSql]
 * and [expectedIsVarcharHonorParamsSql].
 */
private fun isStringTypeTestCase(
    sql: String,
    expectedIsCharHonorParamsSql: String,
    expectedIsVarcharHonorParamsSql: String
) =
    listOf(
        EvaluatorTestCase(
            sql.replace("{TYPE}", "CHAR"),
            expectedIsCharHonorParamsSql,
            compileOptionsBuilderBlock = CompOptions.STANDARD.optionsBlock
        ),
        EvaluatorTestCase(
            sql.replace("{TYPE}", "CHARACTER"),
            expectedIsCharHonorParamsSql,
            compileOptionsBuilderBlock = CompOptions.STANDARD.optionsBlock
        ),
        EvaluatorTestCase(
            sql.replace("{TYPE}", "VARCHAR"),
            expectedIsVarcharHonorParamsSql,
            compileOptionsBuilderBlock = CompOptions.STANDARD.optionsBlock
        ),
        EvaluatorTestCase(
            sql.replace("{TYPE}", "CHARACTER VARYING"),
            expectedIsVarcharHonorParamsSql,
            compileOptionsBuilderBlock = CompOptions.STANDARD.optionsBlock
        )
    )

/**
 * Extends `isStringTypeTestCase`, applying [sqlTemplate] to each string in [strings].
 *
 * Returns 4 tests cases for every string.
 */
private fun isUnicodeStringTestCase(
    strings: List<String>,
    sqlTemplate: String,
    expectedIsCharHonorParamsSql: String,
    expectedIsVarcharHonorParamsSql: String
) =
    strings.map {
        isStringTypeTestCase(
            sql = sqlTemplate.replace("<STRING>", it),
            expectedIsCharHonorParamsSql = expectedIsCharHonorParamsSql,
            expectedIsVarcharHonorParamsSql = expectedIsVarcharHonorParamsSql
        )
    }.flatten()

/**
 * Constructs 4 test cases for the specified cast expression, replacing "{TYPE}" in [sql] with
 * `DECIMAL`, or `NUMERIC`.
 */
private fun isDecimalTypeTestCase(
    sql: String,
    expectedIsDecimalHonorParamsResult: String
) =
    listOf(
        EvaluatorTestCase(
            sql.replace("{TYPE}", "DECIMAL"),
            expectedIsDecimalHonorParamsResult,
            compileOptionsBuilderBlock = CompOptions.STANDARD.optionsBlock
        ),
        EvaluatorTestCase(
            sql.replace("{TYPE}", "NUMERIC"),
            expectedIsDecimalHonorParamsResult,
            compileOptionsBuilderBlock = CompOptions.STANDARD.optionsBlock
        )
    )

/**
 * Returns 2 test cases for each mode.
 */
private fun isIntDecimalTypeTestCase(
    sql: String,
    expectedHonorParamsResult: String
) = listOf(
    EvaluatorTestCase(
        sql,
        expectedHonorParamsResult,
        compileOptionsBuilderBlock = CompOptions.STANDARD.optionsBlock
    )
)

private fun isTimestampTypeTestCase(
    sql: String,
    expectedHonorParamsResult: String
) = listOf(
    EvaluatorTestCase(
        sql,
        expectedHonorParamsResult,
        compileOptionsBuilderBlock = CompOptions.STANDARD.optionsBlock
    )
)

/** Tests for `IS` operator. */
class EvaluatingCompilerIsTests : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(BasicIsOperatorTests::class)
    fun basicIsOperatorTests(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())

    class BasicIsOperatorTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            EvaluatorTestCase(
                query = "MISSING IS MISSING",
                expectedResult = "TRUE"
            ),
            EvaluatorTestCase(
                query = "MISSING IS NULL",
                expectedResult = "TRUE"
            ),
            EvaluatorTestCase(
                query = "NULL IS NOT MISSING",
                expectedResult = "TRUE"
            ),
            EvaluatorTestCase(
                query = "NULL IS NOT NULL",
                expectedResult = "FALSE"
            ),
            EvaluatorTestCase(
                query = "`null.string` IS NOT NULL",
                expectedResult = "FALSE"
            ),
            EvaluatorTestCase(
                query = "'' IS STRING",
                expectedResult = "TRUE"
            ),
            EvaluatorTestCase(
                query = "'' IS VARCHAR",
                expectedResult = "TRUE"
            ),
            EvaluatorTestCase(
                query = "'hello' IS VARCHAR",
                expectedResult = "TRUE"
            ),
            EvaluatorTestCase(
                query = "'hello' IS CHARACTER VARYING",
                expectedResult = "TRUE"
            ),
            EvaluatorTestCase(
                query = "'hello' IS STRING",
                expectedResult = "TRUE"
            ),
            EvaluatorTestCase(
                query = "50000 IS NOT INT",
                expectedResult = "FALSE"
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(SizedIntegerTests::class)
    fun sizedIntegerTests(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())

    class SizedIntegerTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> {
            fun constrainedIntCases(typeName: String, minValue: Long, maxValue: Long, includeOufRangeTests: Boolean) =
                listOfNotNull(
                    isIntDecimalTypeTestCase(
                        "1 IS $typeName",
                        expectedHonorParamsResult = "TRUE"
                    ),
                    isIntDecimalTypeTestCase(
                        "-1 IS $typeName",
                        expectedHonorParamsResult = "TRUE"
                    ),

                    isIntDecimalTypeTestCase(
                        "$minValue IS $typeName",
                        expectedHonorParamsResult = "TRUE"
                    ),
                    isIntDecimalTypeTestCase(
                        "${minValue - 1} IS $typeName",
                        expectedHonorParamsResult = "FALSE"
                    ).takeIf { includeOufRangeTests },

                    isIntDecimalTypeTestCase(
                        "$maxValue IS $typeName",
                        expectedHonorParamsResult = "TRUE"
                    ),

                    isIntDecimalTypeTestCase(
                        "${maxValue + 1} IS $typeName",
                        expectedHonorParamsResult = "FALSE"
                    ).takeIf { includeOufRangeTests }
                ).flatten()

            return listOf(
                constrainedIntCases("SMALLINT", Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong(), includeOufRangeTests = true),
                constrainedIntCases("INT4", Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong(), includeOufRangeTests = true)
            ).flatten()
        }
    }

    @ParameterizedTest
    @ArgumentsSource(NotAStringCases::class)
    fun notAStringTests(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())
    class NotAStringCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            isStringTypeTestCase(
                sql = "1 IS {TYPE}(1)",
                expectedIsCharHonorParamsSql = "false",
                expectedIsVarcharHonorParamsSql = "false"
            ),
            isStringTypeTestCase(
                sql = "1.0 IS {TYPE}(1)",
                expectedIsCharHonorParamsSql = "false",
                expectedIsVarcharHonorParamsSql = "false"
            ),
            isStringTypeTestCase(
                sql = "[] IS {TYPE}(1)",
                expectedIsCharHonorParamsSql = "false",
                expectedIsVarcharHonorParamsSql = "false"
            ),
            isStringTypeTestCase(
                sql = "{} IS {TYPE}(1)",
                expectedIsCharHonorParamsSql = "false",
                expectedIsVarcharHonorParamsSql = "false"
            ),
            isStringTypeTestCase(
                sql = "<<>> IS {TYPE}(1)",
                expectedIsCharHonorParamsSql = "false",
                expectedIsVarcharHonorParamsSql = "false"
            )
        ).flatten()
    }

    @ParameterizedTest
    @ArgumentsSource(ZeroLengthCases::class)
    fun zeroLengthStringTests(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())

    class ZeroLengthCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            isStringTypeTestCase(
                sql = "'' IS {TYPE}(1)",
                expectedIsCharHonorParamsSql = "FALSE",
                expectedIsVarcharHonorParamsSql = "TRUE"
            ),
            isStringTypeTestCase(
                sql = "'' IS {TYPE}(100)",
                expectedIsCharHonorParamsSql = "FALSE",
                expectedIsVarcharHonorParamsSql = "TRUE"
            )
        ).flatten()
    }

    @ParameterizedTest
    @ArgumentsSource(SingleByteCharacterCases::class)
    fun unicodeIsCharacterTypeTests(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())

    class SingleByteCharacterCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> {
            val fourCharacterStrings = listOf(
                "abcd",
                "💩💩💩💩",
                "😁😞😸😸",
                "話家身圧",
                "💋💋💋💋",
                "a💩😸💋",
                "\u00A2\u0039\uD55C\uD800\uDF48"
            )

            return listOf(
                isUnicodeStringTestCase(
                    strings = listOf("a", "💩", "😁"),
                    sqlTemplate = "'<STRING>' IS {TYPE}(1)",
                    expectedIsCharHonorParamsSql = "TRUE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(2)",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "FALSE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(3)",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "FALSE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(4)",
                    expectedIsCharHonorParamsSql = "TRUE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(5)",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = listOf("a", "💩"),
                    sqlTemplate = "'<STRING>' IS {TYPE}(1)",
                    expectedIsCharHonorParamsSql = "TRUE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(2)",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "FALSE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(3)",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "FALSE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(4)",
                    expectedIsCharHonorParamsSql = "TRUE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(5)",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                )
            ).flatten()
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DecimalIsOperatorTestCases::class)
    fun isDecimalTests(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())

    class DecimalIsOperatorTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            isDecimalTypeTestCase(
                sql = "1 IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ),
            isDecimalTypeTestCase(
                sql = "`1e0` IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ), // Note:  Ion literal is a FLOAT
            isDecimalTypeTestCase(
                sql = "'foo' IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ),
            isDecimalTypeTestCase(
                sql = "`foo` IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ),
            isDecimalTypeTestCase(
                sql = "{} IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ),
            isDecimalTypeTestCase(
                sql = "[] IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ),
            isDecimalTypeTestCase(
                sql = "<<>> IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ),
            isDecimalTypeTestCase(
                sql = "true IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ),
            isDecimalTypeTestCase(
                sql = "false IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "false"
            ),

            // any scale and precision
            isDecimalTypeTestCase(
                sql = "1.0 IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),
            isDecimalTypeTestCase(
                sql = "1. IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),
            isDecimalTypeTestCase(
                sql = ".1 IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),
            isDecimalTypeTestCase(
                sql = "1234567890.0987654321 IS {TYPE}",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // equal scale and precision
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(6, 3)",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // greater precision and scale
            isDecimalTypeTestCase(
                sql = "123.456 IS DECIMAL(7, 4)",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // Equal Precision and scale
            isDecimalTypeTestCase(
                sql = "0.001 is DECIMAL(3,3)",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // Equal Precision and scale
            isDecimalTypeTestCase(
                sql = "1.000 is DECIMAL(4,3)",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // less precision and scale
            isDecimalTypeTestCase(
                sql = "123.456 IS DECIMAL(2, 2)",
                expectedIsDecimalHonorParamsResult = "false"
            ),

            // less precision but equal scale
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(5, 3)",
                expectedIsDecimalHonorParamsResult = "FALSE"
            ),

            // greater precision but equal scale
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(7, 3)",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // equal precision but less scale
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(6, 2)",
                expectedIsDecimalHonorParamsResult = "FALSE"
            ),

            // equal precision but greater scale
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(6, 4)",
                expectedIsDecimalHonorParamsResult = "FALSE"
            ),

            // Leading non-significant integral coefficient should not affect the result.
            isDecimalTypeTestCase(
                sql = "0123.456 IS {TYPE}(6, 3)",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // All zeros.
            isDecimalTypeTestCase(
                sql = "0.00 IS {TYPE}(4, 3)",
                expectedIsDecimalHonorParamsResult = "TRUE"
            )

        ).flatten()
    }

    @ParameterizedTest
    @ArgumentsSource(TimestampIsOperatorTestCases::class)
    fun isTimestampTests(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())

    class TimestampIsOperatorTestCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Without time zone
            // No precision - SQL style
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00' is TIMESTAMP",
                "true"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00' is TIMESTAMP WITH TIME ZONE",
                "false"
            ),
            // No precision - RFC 3339
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01T00:00:00' is TIMESTAMP",
                "true"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01T00:00:00' is TIMESTAMP WITH TIME ZONE",
                "false"
            ),
            // Precision
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000' is TIMESTAMP(5)",
                "true"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000' is TIMESTAMP(5) WITH TIME ZONE",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000' is TIMESTAMP(4)",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000' is TIMESTAMP(4) WITH TIME ZONE",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000' is TIMESTAMP(10)",
                "true"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000' is TIMESTAMP(10) WITH TIME ZONE",
                "false"
            ),
            // With known time zone
            // No precision - SQL style
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00+00:00' is TIMESTAMP",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00+00:00' is TIMESTAMP WITH TIME ZONE",
                "true"
            ),
            // No precision - RFC 3339
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01T00:00:00+00:00' is TIMESTAMP",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01T00:00:00+00:00' is TIMESTAMP WITH TIME ZONE",
                "true"
            ),
            // Precision
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000+00:00' is TIMESTAMP(5)",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000+00:00' is TIMESTAMP(5) WITH TIME ZONE",
                "true"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000+00:00' is TIMESTAMP(4)",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000+00:00' is TIMESTAMP(4) WITH TIME ZONE",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000+00:00' is TIMESTAMP(10)",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000+00:00' is TIMESTAMP(10) WITH TIME ZONE",
                "true"
            ),
            // RFC/ISO special symbol T/Z
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01T00:00:00.00000Z' is TIMESTAMP(10)",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01T00:00:00.00000Z' is TIMESTAMP(10) WITH TIME ZONE",
                "true"
            ),
            // With unknown time zone
            // No precision - SQL style
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00-00:00' is TIMESTAMP",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00-00:00' is TIMESTAMP WITH TIME ZONE",
                "true"
            ),
            // No precision - RFC 3339
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01T00:00:00-00:00' is TIMESTAMP",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01T00:00:00-00:00' is TIMESTAMP WITH TIME ZONE",
                "true"
            ),
            // Precision
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000-00:00' is TIMESTAMP(5)",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000-00:00' is TIMESTAMP(5) WITH TIME ZONE",
                "true"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000-00:00' is TIMESTAMP(4)",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000-00:00' is TIMESTAMP(4) WITH TIME ZONE",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000-00:00' is TIMESTAMP(10)",
                "false"
            ),
            isTimestampTypeTestCase(
                "TIMESTAMP '2023-06-01 00:00:00.00000-00:00' is TIMESTAMP(10) WITH TIME ZONE",
                "true"
            ),
        ).flatten()
    }
}
