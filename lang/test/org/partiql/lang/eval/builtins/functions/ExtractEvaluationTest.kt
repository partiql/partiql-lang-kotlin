package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.toSession
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf

/**
 * Parsing related tests in [org.partiql.lang.syntax.SqlParserTest] and [org.partiql.lang.errors.ParserErrorsTest].
 */
class ExtractEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ExtractPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) = assertEval(
        source = testCase.source,
        expected = testCase.expected,
        session = testCase.session,
        excludeLegacySerializerAssertions = true
    )

    class ExtractPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("extract(year FROM null)", "null"),
            ExprFunctionTestCase("extract(month FROM null)", "null"),
            ExprFunctionTestCase("extract(day FROM null)", "null"),
            ExprFunctionTestCase("extract(hour FROM null)", "null"),
            ExprFunctionTestCase("extract(minute FROM null)", "null"),
            ExprFunctionTestCase("extract(second FROM null)", "null"),
            ExprFunctionTestCase("extract(timezone_hour FROM null)", "null"),
            ExprFunctionTestCase("extract(timezone_minute FROM null)", "null"),
            ExprFunctionTestCase("extract(year FROM missing)", "null"),
            ExprFunctionTestCase("extract(month FROM missing)", "null"),
            ExprFunctionTestCase("extract(day FROM missing)", "null"),
            ExprFunctionTestCase("extract(hour FROM missing)", "null"),
            ExprFunctionTestCase("extract(minute FROM missing)", "null"),
            ExprFunctionTestCase("extract(second FROM missing)", "null"),
            ExprFunctionTestCase("extract(timezone_hour FROM missing)", "null"),
            ExprFunctionTestCase("extract(timezone_minute FROM missing)", "null"),
            ExprFunctionTestCase("extract(second FROM a)", "55.", mapOf("a" to "2017-01-10T05:30:55Z").toSession()),
            // just year
            ExprFunctionTestCase("extract(year FROM `2017T`)", "2017."),
            ExprFunctionTestCase("extract(month FROM `2017T`)", "1."),
            ExprFunctionTestCase("extract(day FROM `2017T`)", "1."),
            ExprFunctionTestCase("extract(hour FROM `2017T`)", "0."),
            ExprFunctionTestCase("extract(minute FROM `2017T`)", "0."),
            ExprFunctionTestCase("extract(second FROM `2017T`)", "0."),
            ExprFunctionTestCase("extract(timezone_hour FROM `2017T`)", "0."),
            ExprFunctionTestCase("extract(timezone_minute FROM `2017T`)", "0."),
            // year, month
            ExprFunctionTestCase("extract(year FROM `2017-01T`)", "2017."),
            ExprFunctionTestCase("extract(month FROM `2017-01T`)", "1."),
            ExprFunctionTestCase("extract(day FROM `2017-01T`)", "1."),
            ExprFunctionTestCase("extract(hour FROM `2017-01T`)", "0."),
            ExprFunctionTestCase("extract(minute FROM `2017-01T`)", "0."),
            ExprFunctionTestCase("extract(second FROM `2017-01T`)", "0."),
            ExprFunctionTestCase("extract(timezone_hour FROM `2017-01T`)", "0."),
            ExprFunctionTestCase("extract(timezone_minute FROM `2017-01T`)", "0."),
            // year, month, day
            ExprFunctionTestCase("extract(year FROM `2017-01-02T`)", "2017."),
            ExprFunctionTestCase("extract(month FROM `2017-01-02T`)", "1."),
            ExprFunctionTestCase("extract(day FROM `2017-01-02T`)", "2."),
            ExprFunctionTestCase("extract(hour FROM `2017-01-02T`)", "0."),
            ExprFunctionTestCase("extract(minute FROM `2017-01-02T`)", "0."),
            ExprFunctionTestCase("extract(second FROM `2017-01-02T`)", "0."),
            ExprFunctionTestCase("extract(timezone_hour FROM `2017-01-02T`)", "0."),
            ExprFunctionTestCase("extract(timezone_minute FROM `2017-01-02T`)", "0."),
            // year, month, day, hour, minute
            ExprFunctionTestCase("extract(year FROM `2017-01-02T03:04Z`)", "2017."),
            ExprFunctionTestCase("extract(month FROM `2017-01-02T03:04Z`)", "1."),
            ExprFunctionTestCase("extract(day FROM `2017-01-02T03:04Z`)", "2."),
            ExprFunctionTestCase("extract(hour FROM `2017-01-02T03:04Z`)", "3."),
            ExprFunctionTestCase("extract(minute FROM `2017-01-02T03:04Z`)", "4."),
            ExprFunctionTestCase("extract(second FROM `2017-01-02T03:04Z`)", "0."),
            ExprFunctionTestCase("extract(timezone_hour FROM `2017-01-02T03:04Z`)", "0."),
            ExprFunctionTestCase("extract(timezone_minute FROM `2017-01-02T03:04Z`)", "0."),
            // year, month, day, hour, minute, second
            ExprFunctionTestCase("extract(year FROM `2017-01-02T03:04:05Z`)", "2017."),
            ExprFunctionTestCase("extract(month FROM `2017-01-02T03:04:05Z`)", "1."),
            ExprFunctionTestCase("extract(day FROM `2017-01-02T03:04:05Z`)", "2."),
            ExprFunctionTestCase("extract(hour FROM `2017-01-02T03:04:05Z`)", "3."),
            ExprFunctionTestCase("extract(minute FROM `2017-01-02T03:04:05Z`)", "4."),
            ExprFunctionTestCase("extract(second FROM `2017-01-02T03:04:05Z`)", "5."),
            ExprFunctionTestCase("extract(timezone_hour FROM `2017-01-02T03:04:05Z`)", "0."),
            ExprFunctionTestCase("extract(timezone_minute FROM `2017-01-02T03:04:05Z`)", "0."),
            // year, month, day, hour, minute, second, local offset
            ExprFunctionTestCase("extract(year FROM `2017-01-02T03:04:05+07:08`)", "2017."),
            ExprFunctionTestCase("extract(month FROM `2017-01-02T03:04:05+07:08`)", "1."),
            ExprFunctionTestCase("extract(day FROM `2017-01-02T03:04:05+07:08`)", "2."),
            ExprFunctionTestCase("extract(hour FROM `2017-01-02T03:04:05+07:08`)", "3."),
            ExprFunctionTestCase("extract(minute FROM `2017-01-02T03:04:05+07:08`)", "4."),
            ExprFunctionTestCase("extract(second FROM `2017-01-02T03:04:05+07:08`)", "5."),
            ExprFunctionTestCase("extract(timezone_hour FROM `2017-01-02T03:04:05+07:08`)", "7."),
            ExprFunctionTestCase("extract(timezone_minute FROM `2017-01-02T03:04:05+07:08`)", "8."),
            // negative offset
            ExprFunctionTestCase("extract(timezone_hour FROM `2017-01-02T03:04:05-07:08`)", "-7."),
            ExprFunctionTestCase("extract(timezone_minute FROM `2017-01-02T03:04:05-07:08`)", "-8."),
            // DATE
            ExprFunctionTestCase("extract(year FROM DATE '2012-12-12')", "2012."),
            ExprFunctionTestCase("extract(month FROM DATE '2012-12-12')", "12."),
            ExprFunctionTestCase("extract(day FROM DATE '2012-12-12')", "12."),
            ExprFunctionTestCase("extract(hour FROM DATE '2012-12-12')", "0."),
            ExprFunctionTestCase("extract(minute FROM DATE '2012-12-12')", "0."),
            ExprFunctionTestCase("extract(second FROM DATE '2012-12-12')", "0."),
            // TIME
            ExprFunctionTestCase("extract(hour FROM TIME '23:12:59.128')", "23."),
            ExprFunctionTestCase("extract(minute FROM TIME '23:12:59.128')", "12."),
            ExprFunctionTestCase("extract(second FROM TIME '23:12:59.128')", "59.128"),
            ExprFunctionTestCase("extract(hour FROM TIME (2) '23:12:59.128')", "23."),
            ExprFunctionTestCase("extract(minute FROM TIME (2) '23:12:59.128')", "12."),
            ExprFunctionTestCase("extract(second FROM TIME (2) '23:12:59.128')", "59.13"),
            // TIME WITH TIME ZONE
            ExprFunctionTestCase("extract(hour FROM TIME WITH TIME ZONE '23:12:59.128-06:30')", "23."),
            ExprFunctionTestCase("extract(minute FROM TIME WITH TIME ZONE '23:12:59.128-06:30')", "12."),
            ExprFunctionTestCase("extract(second FROM TIME WITH TIME ZONE '23:12:59.128-06:30')", "59.128"),
            ExprFunctionTestCase("extract(timezone_hour FROM TIME WITH TIME ZONE '23:12:59.128-06:30')", "-6."),
            ExprFunctionTestCase("extract(timezone_minute FROM TIME WITH TIME ZONE '23:12:59.128-06:30')", "-30."),
            ExprFunctionTestCase("extract(hour FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')", "23."),
            ExprFunctionTestCase("extract(minute FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')", "12."),
            ExprFunctionTestCase("extract(second FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')", "59.13"),
            ExprFunctionTestCase("extract(timezone_hour FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')", "-6."),
            ExprFunctionTestCase("extract(timezone_minute FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')", "-30.")
        )
    }

    // Invalid arguments
    data class InvalidArgTestCase(
        val query: String,
        val message: String
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun extractInvalidArgumentTests(testCase: InvalidArgTestCase) =
        assertThrows(
            testCase.query,
            ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            expectedErrorContext = propertyValueMapOf(1, 1),
            expectedPermissiveModeResult = "MISSING",
            excludeLegacySerializerAssertions = true
        )

    class InvalidArgCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // DATE
            InvalidArgTestCase(
                "EXTRACT(timezone_hour FROM DATE '2012-12-12')",
                "Timestamp unit timezone_hour not supported for DATE type"
            ),
            InvalidArgTestCase(
                "EXTRACT(timezone_minute FROM DATE '2012-12-12')",
                "Timestamp unit timezone_minute not supported for DATE type"
            ),
            // TIME
            InvalidArgTestCase("EXTRACT(year FROM TIME '23:12:59.128')", "Time unit year not supported for TIME type."),
            InvalidArgTestCase(
                "EXTRACT(month FROM TIME '23:12:59.128')",
                "Time unit month not supported for TIME type."
            ),
            InvalidArgTestCase("EXTRACT(day FROM TIME '23:12:59.128')", "Time unit day not supported for TIME type."),
            InvalidArgTestCase(
                "EXTRACT(timezone_hour FROM TIME '23:12:59.128')",
                "Time unit timezone_hour not supported for TIME type without TIME ZONE"
            ),
            InvalidArgTestCase(
                "EXTRACT(timezone_minute FROM TIME '23:12:59.128')",
                "Time unit timezone_minute not supported for TIME type without TIME ZONE"
            ),
            InvalidArgTestCase(
                "EXTRACT(year FROM TIME (2) '23:12:59.128')",
                "Time unit year not supported for TIME type."
            ),
            InvalidArgTestCase(
                "EXTRACT(month FROM TIME (2) '23:12:59.128')",
                "Time unit month not supported for TIME type."
            ),
            InvalidArgTestCase(
                "EXTRACT(day FROM TIME (2) '23:12:59.128')",
                "Time unit day not supported for TIME type."
            ),
            InvalidArgTestCase(
                "EXTRACT(timezone_hour FROM TIME (2) '23:12:59.128')",
                "Time unit timezone_hour not supported for TIME type without TIME ZONE"
            ),
            InvalidArgTestCase(
                "EXTRACT(timezone_minute FROM TIME (2) '23:12:59.128')",
                "Time unit timezone_minute not supported for TIME type without TIME ZONE"
            ),
            // TIME WITH TIME ZONE
            InvalidArgTestCase(
                "EXTRACT(year FROM TIME WITH TIME ZONE '23:12:59.128-06:30')",
                "Time unit year not supported for TIME type."
            ),
            InvalidArgTestCase(
                "EXTRACT(month FROM TIME WITH TIME ZONE '23:12:59.128-06:30')",
                "Time unit month not supported for TIME type."
            ),
            InvalidArgTestCase(
                "EXTRACT(day FROM TIME WITH TIME ZONE '23:12:59.128-06:30')",
                "Time unit day not supported for TIME type."
            ),
            InvalidArgTestCase(
                "EXTRACT(year FROM TIME (2) '23:12:59.128')",
                "Time unit year not supported for TIME type."
            ),
            InvalidArgTestCase(
                "EXTRACT(month FROM TIME (2) '23:12:59.128')",
                "Time unit month not supported for TIME type."
            ),
            InvalidArgTestCase(
                "EXTRACT(day FROM TIME (2) '23:12:59.128')",
                "Time unit day not supported for TIME type."
            )
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun extractInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "extract",
        syntaxSuffix = "(year from ",
        args = listOf(
            Argument(2, StaticType.unionOf(StaticType.TIMESTAMP, StaticType.TIME, StaticType.DATE), ")")
        )
    )

    // The invalid arity check is considered as syntax error and already done in the ParserErrorsTest.kt
}
