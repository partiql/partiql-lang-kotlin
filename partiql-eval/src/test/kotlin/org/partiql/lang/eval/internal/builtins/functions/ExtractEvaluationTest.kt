package org.partiql.lang.eval.internal.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.internal.builtins.Argument
import org.partiql.lang.eval.internal.builtins.toSession
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.types.StaticType

/**
 * Parsing related tests in [org.partiql.lang.syntax.PartiQLParserTest] and [org.partiql.errors.ParserErrorsTest].
 */
class ExtractEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ExtractPassCases::class)
    fun runPassTests(testCase: org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase) = runEvaluatorTestCase(
        query = testCase.source,
        session = testCase.session,
        expectedResult = testCase.expectedLegacyModeResult,
        expectedPermissiveModeResult = testCase.expectedPermissiveModeResult
    )

    class ExtractPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(year FROM null)", "null"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(month FROM null)", "null"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(day FROM null)", "null"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(hour FROM null)", "null"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(minute FROM null)", "null"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(second FROM null)", "null"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(timezone_hour FROM null)", "null"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(timezone_minute FROM null)", "null"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(year FROM missing)",
                "null",
                "$MISSING_ANNOTATION::null"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(month FROM missing)",
                "null",
                "$MISSING_ANNOTATION::null"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(day FROM missing)",
                "null",
                "$MISSING_ANNOTATION::null"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(hour FROM missing)",
                "null",
                "$MISSING_ANNOTATION::null"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM missing)",
                "null",
                "$MISSING_ANNOTATION::null"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM missing)",
                "null",
                "$MISSING_ANNOTATION::null"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM missing)",
                "null",
                "$MISSING_ANNOTATION::null"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM missing)",
                "null",
                "$MISSING_ANNOTATION::null"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM a)",
                "55.",
                session = mapOf("a" to "2017-01-10T05:30:55Z").toSession()
            ),
            // just year
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(year FROM `2017T`)", "2017."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(month FROM `2017T`)", "1."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(day FROM `2017T`)", "1."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(hour FROM `2017T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(minute FROM `2017T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(second FROM `2017T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(timezone_hour FROM `2017T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(timezone_minute FROM `2017T`)", "0."),
            // year, month
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(year FROM `2017-01T`)", "2017."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(month FROM `2017-01T`)", "1."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(day FROM `2017-01T`)", "1."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(hour FROM `2017-01T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(minute FROM `2017-01T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(second FROM `2017-01T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM `2017-01T`)",
                "0."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM `2017-01T`)",
                "0."
            ),
            // year, month, day
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(year FROM `2017-01-02T`)", "2017."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(month FROM `2017-01-02T`)", "1."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(day FROM `2017-01-02T`)", "2."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(hour FROM `2017-01-02T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(minute FROM `2017-01-02T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(second FROM `2017-01-02T`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM `2017-01-02T`)",
                "0."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM `2017-01-02T`)",
                "0."
            ),
            // year, month, day, hour, minute
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(year FROM `2017-01-02T03:04Z`)",
                "2017."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(month FROM `2017-01-02T03:04Z`)",
                "1."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(day FROM `2017-01-02T03:04Z`)", "2."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(hour FROM `2017-01-02T03:04Z`)",
                "3."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM `2017-01-02T03:04Z`)",
                "4."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM `2017-01-02T03:04Z`)",
                "0."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM `2017-01-02T03:04Z`)",
                "0."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM `2017-01-02T03:04Z`)",
                "0."
            ),
            // year, month, day, hour, minute, second
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(year FROM `2017-01-02T03:04:05Z`)",
                "2017."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(month FROM `2017-01-02T03:04:05Z`)",
                "1."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(day FROM `2017-01-02T03:04:05Z`)",
                "2."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(hour FROM `2017-01-02T03:04:05Z`)",
                "3."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM `2017-01-02T03:04:05Z`)",
                "4."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM `2017-01-02T03:04:05Z`)",
                "5."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM `2017-01-02T03:04:05Z`)",
                "0."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM `2017-01-02T03:04:05Z`)",
                "0."
            ),
            // year, month, day, hour, minute, second, local offset
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(year FROM `2017-01-02T03:04:05+07:08`)",
                "2017."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(month FROM `2017-01-02T03:04:05+07:08`)",
                "1."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(day FROM `2017-01-02T03:04:05+07:08`)",
                "2."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(hour FROM `2017-01-02T03:04:05+07:08`)",
                "3."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM `2017-01-02T03:04:05+07:08`)",
                "4."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM `2017-01-02T03:04:05+07:08`)",
                "5."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM `2017-01-02T03:04:05+07:08`)",
                "7."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM `2017-01-02T03:04:05+07:08`)",
                "8."
            ),
            // negative offset
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM `2017-01-02T03:04:05-07:08`)",
                "-7."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM `2017-01-02T03:04:05-07:08`)",
                "-8."
            ),
            // DATE
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(year FROM DATE '2012-12-12')",
                "2012."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(month FROM DATE '2012-12-12')",
                "12."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(day FROM DATE '2012-12-12')", "12."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("extract(hour FROM DATE '2012-12-12')", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM DATE '2012-12-12')",
                "0."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM DATE '2012-12-12')",
                "0."
            ),
            // TIME
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(hour FROM TIME '23:12:59.128')",
                "23."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM TIME '23:12:59.128')",
                "12."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM TIME '23:12:59.128')",
                "59.128"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(hour FROM TIME (2) '23:12:59.128')",
                "23."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM TIME (2) '23:12:59.128')",
                "12."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM TIME (2) '23:12:59.128')",
                "59.13"
            ),
            // TIME WITH TIME ZONE
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(hour FROM TIME WITH TIME ZONE '23:12:59.128-06:30')",
                "23."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM TIME WITH TIME ZONE '23:12:59.128-06:30')",
                "12."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM TIME WITH TIME ZONE '23:12:59.128-06:30')",
                "59.128"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM TIME WITH TIME ZONE '23:12:59.128-06:30')",
                "-6."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM TIME WITH TIME ZONE '23:12:59.128-06:30')",
                "-30."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(hour FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')",
                "23."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(minute FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')",
                "12."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(second FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')",
                "59.13"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_hour FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')",
                "-6."
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "extract(timezone_minute FROM TIME (2) WITH TIME ZONE '23:12:59.128-06:30')",
                "-30."
            )
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
        runEvaluatorErrorTestCase(
            testCase.query,
            ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            expectedErrorContext = propertyValueMapOf(1, 1),
            expectedPermissiveModeResult = "MISSING"
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
    fun extractInvalidArgTypeTest() = org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
        funcName = "extract",
        syntaxSuffix = "(year from ",
        args = listOf(
            Argument(2, StaticType.unionOf(StaticType.TIMESTAMP, StaticType.TIME, StaticType.DATE), ")")
        )
    )

    // The invalid arity check is considered as syntax error and already done in the ParserErrorsTest.kt
}
