package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to
import org.partiql.types.StaticType

class ToStringEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ToStringPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, expectedResult = testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

    class ToStringPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // Note that the amount of testing here is a bit on the light side because most of the testing for the formatting
            // functionality behind `to_string` is in TimestampTemporalAccessorTests.
            ExprFunctionTestCase("to_string(`0500-03-09`, 'MM/dd/yyyy')", "\"03/09/0500\""),
            ExprFunctionTestCase("to_string(`0500-03-09`, 'M/d/y')", "\"3/9/500\""),
            ExprFunctionTestCase("to_string(`0001-03-09`, 'MM/dd/yyyy')", "\"03/09/0001\""),
            ExprFunctionTestCase("to_string(`0001-03-09`, 'M/d/y')", "\"3/9/1\""),
            ExprFunctionTestCase("to_string(`9999-03-09`, 'MM/dd/yyyy')", "\"03/09/9999\""),
            ExprFunctionTestCase("to_string(`9999-03-09`, 'M/d/y')", "\"3/9/9999\""),
            ExprFunctionTestCase("to_string(`0001-03-09`, 'y')", "\"1\""),
            ExprFunctionTestCase("to_string(`9999-03-09`, null)", "null"),
            ExprFunctionTestCase("to_string(null, 'M/d/y')", "null"),
            ExprFunctionTestCase("to_string(`9999-03-09`, missing)", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("to_string(missing, 'M/d/y')", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18Z`, 'MMMM d, y')", "\"July 20, 1969\""),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18Z`, 'MMM d, yyyy')", "\"Jul 20, 1969\""),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18Z`, 'M-d-yy')", "\"7-20-69\""),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18Z`, 'MM-d-y')", "\"07-20-1969\""),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18Z`, 'MMMM d, y h:m a')", "\"July 20, 1969 8:18 PM\""),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18Z`, 'y-MM-dd''T''H:m:ssX')", "\"1969-07-20T20:18:00Z\""),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssX')", "\"1969-07-20T20:18:00+08\""),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXX')", "\"1969-07-20T20:18:00+0800\""),
            ExprFunctionTestCase("to_string(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXXX')", "\"1969-07-20T20:18:00+08:00\"")
        )
    }

    // Error test cases: Invalid arguments
    data class InvalidArgTestCase(
        val source: String,
        val invalidTimeFormatPattern: String
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun toStringInvalidArgumentTests(testCase: InvalidArgTestCase) = runEvaluatorErrorTestCase(
        testCase.source,
        ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
        propertyValueMapOf(
            1, 1,
            Property.TIMESTAMP_FORMAT_PATTERN to testCase.invalidTimeFormatPattern
        ),
        expectedPermissiveModeResult = "MISSING"
    )

    class InvalidArgCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            InvalidArgTestCase("to_string(`2017-01-01`, 'b')", "b"),

            // Symbol 'z' is known to Java's DateTimeFormatter but is not handled by TimestampTemporalAccessor
            InvalidArgTestCase("to_string(`2017-01-01`, 'Y')", "Y"),

            // Symbol 'VV' is known to Java's DateTimeFormatter but is not handled by TimestampTemporalAccessor
            // *and* causes a different exception to be thrown by DateTimeFormatter.format() than 'z'
            InvalidArgTestCase("to_string(`2017-01-01`, 'VV')", "VV")
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun toStringInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "to_string",
        args = listOf(
            Argument(1, StaticType.TIMESTAMP, ","),
            Argument(2, StaticType.STRING, ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun toStringInvalidArityTest() = checkInvalidArity(
        funcName = "to_string",
        maxArity = 2,
        minArity = 2
    )
}
