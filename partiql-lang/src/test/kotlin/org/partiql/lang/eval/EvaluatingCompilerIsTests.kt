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
 *
 * When unspecified, the values of [expectedIsCharHonorParamsSql] and [expectedIsVarcharHonorParamsSql] default to
 * [expectedLegacyResult].
 */
private fun isStringTypeTestCase(
    sql: String,
    expectedLegacyResult: String,
    expectedIsCharHonorParamsSql: String = expectedLegacyResult,
    expectedIsVarcharHonorParamsSql: String = expectedLegacyResult
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
    expectedLegacyResult: String,
    expectedIsCharHonorParamsSql: String = expectedLegacyResult,
    expectedIsVarcharHonorParamsSql: String = expectedLegacyResult
) =
    strings.map {
        isStringTypeTestCase(
            sql = sqlTemplate.replace("<STRING>", it),
            expectedLegacyResult = expectedLegacyResult,
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
    expectedLegacyResult: String,
    expectedIsDecimalHonorParamsResult: String = expectedLegacyResult
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
    expectedLegacyResult: String,
    expectedHonorParamsResult: String = expectedLegacyResult
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
                        expectedLegacyResult = "TRUE",
                        expectedHonorParamsResult = "TRUE"
                    ),
                    isIntDecimalTypeTestCase(
                        "-1 IS $typeName",
                        expectedLegacyResult = "TRUE",
                        expectedHonorParamsResult = "TRUE"
                    ),

                    isIntDecimalTypeTestCase(
                        "$minValue IS $typeName",
                        expectedLegacyResult = "TRUE",
                        expectedHonorParamsResult = "TRUE"
                    ),
                    isIntDecimalTypeTestCase(
                        "${minValue - 1} IS $typeName",
                        expectedLegacyResult = "TRUE",
                        expectedHonorParamsResult = "FALSE"
                    ).takeIf { includeOufRangeTests },

                    isIntDecimalTypeTestCase(
                        "$maxValue IS $typeName",
                        expectedLegacyResult = "TRUE",
                        expectedHonorParamsResult = "TRUE"
                    ),

                    isIntDecimalTypeTestCase(
                        "${maxValue + 1} IS $typeName",
                        expectedLegacyResult = "TRUE",
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
                expectedLegacyResult = "FALSE"
            ),
            isStringTypeTestCase(
                sql = "1.0 IS {TYPE}(1)",
                expectedLegacyResult = "FALSE"
            ),
            isStringTypeTestCase(
                sql = "[] IS {TYPE}(1)",
                expectedLegacyResult = "FALSE"
            ),
            isStringTypeTestCase(
                sql = "{} IS {TYPE}(1)",
                expectedLegacyResult = "FALSE"
            ),
            isStringTypeTestCase(
                sql = "<<>> IS {TYPE}(1)",
                expectedLegacyResult = "FALSE"
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
                expectedLegacyResult = "TRUE",
                expectedIsCharHonorParamsSql = "FALSE",
                expectedIsVarcharHonorParamsSql = "TRUE"
            ),
            isStringTypeTestCase(
                sql = "'' IS {TYPE}(100)",
                expectedLegacyResult = "TRUE",
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
                    expectedLegacyResult = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(2)",
                    expectedLegacyResult = "TRUE",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "FALSE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(3)",
                    expectedLegacyResult = "TRUE",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "FALSE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(4)",
                    expectedLegacyResult = "TRUE",
                    expectedIsCharHonorParamsSql = "TRUE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(5)",
                    expectedLegacyResult = "TRUE",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = listOf("a", "💩"),
                    sqlTemplate = "'<STRING>' IS {TYPE}(1)",
                    expectedLegacyResult = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(2)",
                    expectedLegacyResult = "TRUE",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "FALSE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(3)",
                    expectedLegacyResult = "TRUE",
                    expectedIsCharHonorParamsSql = "FALSE",
                    expectedIsVarcharHonorParamsSql = "FALSE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(4)",
                    expectedLegacyResult = "TRUE",
                    expectedIsCharHonorParamsSql = "TRUE",
                    expectedIsVarcharHonorParamsSql = "TRUE"
                ),
                isUnicodeStringTestCase(
                    strings = fourCharacterStrings,
                    sqlTemplate = "'<STRING>' IS {TYPE}(5)",
                    expectedLegacyResult = "TRUE",
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
                expectedLegacyResult = "FALSE"
            ),
            isDecimalTypeTestCase(
                sql = "`1e0` IS {TYPE}",
                expectedLegacyResult = "FALSE"
            ), // Note:  Ion literal is a FLOAT )
            isDecimalTypeTestCase(
                sql = "'foo' IS {TYPE}",
                expectedLegacyResult = "FALSE"
            ),
            isDecimalTypeTestCase(
                sql = "`foo` IS {TYPE}",
                expectedLegacyResult = "FALSE"
            ),
            isDecimalTypeTestCase("{} IS {TYPE}", "FALSE"),
            isDecimalTypeTestCase(
                sql = "[] IS {TYPE}",
                expectedLegacyResult = "FALSE"
            ),
            isDecimalTypeTestCase(
                sql = "<<>> IS {TYPE}",
                expectedLegacyResult = "FALSE"
            ),
            isDecimalTypeTestCase(
                sql = "true IS {TYPE}",
                expectedLegacyResult = "FALSE"
            ),
            isDecimalTypeTestCase(
                sql = "false IS {TYPE}",
                expectedLegacyResult = "FALSE"
            ),

            // any scale and precision
            isDecimalTypeTestCase(
                sql = "1.0 IS {TYPE}",
                expectedLegacyResult = "TRUE"
            ),
            isDecimalTypeTestCase(
                sql = "1. IS {TYPE}",
                expectedLegacyResult = "TRUE"
            ),
            isDecimalTypeTestCase(
                sql = ".1 IS {TYPE}",
                expectedLegacyResult = "TRUE"
            ),
            isDecimalTypeTestCase(
                sql = "1234567890.0987654321 IS {TYPE}",
                expectedLegacyResult = "TRUE"
            ),

            // equal scale and precision
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(6, 3)",
                expectedLegacyResult = "TRUE"
            ),

            // greater precision and scale
            isDecimalTypeTestCase(
                sql = "123.456 IS DECIMAL(7, 4)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // Equal Precision and scale
            isDecimalTypeTestCase(
                sql = "0.001 is DECIMAL(3,3)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // Equal Precision and scale
            isDecimalTypeTestCase(
                sql = "1.000 is DECIMAL(4,3)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // less precision and scale
            isDecimalTypeTestCase(
                sql = "123.456 IS DECIMAL(2, 2)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "false"
            ),

            // less precision but equal scale
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(5, 3)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "FALSE"
            ),

            // greater precision but equal scale
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(7, 3)",
                expectedLegacyResult = "TRUE"
            ),

            // equal precision but less scale
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(6, 2)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "FALSE"
            ),

            // equal precision but greater scale
            isDecimalTypeTestCase(
                sql = "123.456 IS {TYPE}(6, 4)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "FALSE"
            ),

            // Leading non-significant integral coefficient should not effect the result.
            isDecimalTypeTestCase(
                sql = "0123.456 IS {TYPE}(6, 3)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "TRUE"
            ),

            // All zeros.
            isDecimalTypeTestCase(
                sql = "0.00 IS {TYPE}(4, 3)",
                expectedLegacyResult = "TRUE",
                expectedIsDecimalHonorParamsResult = "TRUE"
            )

        ).flatten()
    }
}
