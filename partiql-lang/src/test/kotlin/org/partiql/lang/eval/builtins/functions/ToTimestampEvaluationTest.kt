package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to
import org.partiql.types.StaticType

class ToTimestampEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ToTimestampPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(
            testCase.source,
            expectedResult = testCase.expectedLegacyModeResult,
            expectedPermissiveModeResult = testCase.expectedPermissiveModeResult,
            expectedResultFormat = ExpectedResultFormat.STRICT
        )

    class ToTimestampPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("to_timestamp('1969-07-20T20:18:00Z')", "TIMESTAMP '1969-07-20T20:18:00Z'"),
            ExprFunctionTestCase("to_timestamp('July 20, 1969', 'MMMM d, y')", "TIMESTAMP '1969-07-20T00:00:00'"),
            ExprFunctionTestCase("to_timestamp('Jul 20, 1969', 'MMM d, yyyy')", "TIMESTAMP '1969-07-20T00:00:00'"),
            ExprFunctionTestCase("to_timestamp('1969-07-20T20:18Z', 'yyyy-MM-dd''T''HH:mmX')", "TIMESTAMP '1969-07-20T20:18:00Z'"),
            ExprFunctionTestCase("to_timestamp('July 20, 1969 8:18 PM', 'MMMM d, y h:m a')", "TIMESTAMP '1969-07-20T20:18:00'"),
            ExprFunctionTestCase("to_timestamp('1969-07-20T20:18:00Z', 'yyyy-MM-dd''T''H:m:ssX')", "TIMESTAMP '1969-07-20T20:18:00Z'"),
            ExprFunctionTestCase("to_timestamp('1969-07-20T20:18:01+08', 'yyyy-MM-dd''T''H:m:ssX')", "TIMESTAMP '1969-07-20T20:18:01+08:00'"),
            ExprFunctionTestCase(
                "to_timestamp('1969-07-20T20:18:02+0800', 'yyyy-MM-dd''T''H:m:ssXXXX')",
                "TIMESTAMP '1969-07-20T20:18:02+08:00'"
            ),
            ExprFunctionTestCase(
                "to_timestamp('1969-07-20T20:18:03+08:00', 'yyyy-MM-dd''T''H:m:ssXXXXX')",
                "TIMESTAMP '1969-07-20T20:18:03+08:00'"
            ),
            ExprFunctionTestCase("to_timestamp('1969-07-20T20:18:00Z')", "TIMESTAMP '1969-07-20T20:18:00Z'"),
            ExprFunctionTestCase("to_timestamp('1969-07-20T20:18:03+08:00')", "TIMESTAMP '1969-07-20T20:18:03+08:00'"),
            ExprFunctionTestCase("to_timestamp(null)", "null"),
            ExprFunctionTestCase("to_timestamp(null, 'M-d-yyyy')", "null"),
            ExprFunctionTestCase("to_timestamp('07-20-1969', null)", "null"),
            ExprFunctionTestCase("to_timestamp(null, null)", "null"),
            ExprFunctionTestCase("to_timestamp(missing)", "null", "missing"),
            ExprFunctionTestCase("to_timestamp(missing, 'M-d-yyyy')", "null", "missing"),
            ExprFunctionTestCase("to_timestamp('07-20-1969', missing)", "null", "missing"),
            ExprFunctionTestCase("to_timestamp(null, null)", "null")
        )
    }

    // Invalid arguments
    @Test
    fun to_timestamp_invalid_ion_timestamp() {
        runEvaluatorErrorTestCase(
            "to_timestamp('not a valid timestamp')",
            ErrorCode.EVALUATOR_TIMESTAMP_PARSE_FAILURE,
            propertyValueMapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )
    }

    @Test
    fun to_timestamp_empty_format_pattern() {
        runEvaluatorErrorTestCase(
            "to_timestamp('doesnt matter', '')",
            ErrorCode.EVALUATOR_INCOMPLETE_TIMESTAMP_FORMAT_PATTERN,
            propertyValueMapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.TIMESTAMP_FORMAT_PATTERN to "",
                Property.TIMESTAMP_FORMAT_PATTERN_FIELDS to "YEAR"
            ),
            expectedPermissiveModeResult = "MISSING"
        )
    }

    @Test
    fun to_timestamp_invalid_format_pattern() {
        runEvaluatorErrorTestCase(
            "to_timestamp('doesnt matter', 'asdfasdfasdf')",
            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_TOKEN,
            propertyValueMapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.TIMESTAMP_FORMAT_PATTERN to "asdfasdfasdf"
            ),
            expectedPermissiveModeResult = "MISSING"
        )
    }

    @Test
    fun to_timestamp_invalid_timestamp() {
        runEvaluatorErrorTestCase(
            "to_timestamp('asdf', 'yyyy')",
            ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE,
            propertyValueMapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.TIMESTAMP_FORMAT_PATTERN to "yyyy"
            ),
            expectedPermissiveModeResult = "MISSING"
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun toTimestampInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "to_timestamp",
        args = listOf(
            Argument(1, StaticType.STRING, ","),
            Argument(2, StaticType.STRING, ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun toTimestampInvalidArityTest() = checkInvalidArity(
        funcName = "to_timestamp",
        minArity = 1,
        maxArity = 2
    )
}
