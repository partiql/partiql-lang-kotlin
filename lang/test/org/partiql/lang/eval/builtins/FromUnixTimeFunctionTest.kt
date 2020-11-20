package org.partiql.lang.eval.builtins

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.util.ArgumentsProviderBase

data class FromUnixTimeTestCase(val unixTimestamp: String, val expected: String)

class FromUnixTimeFunctionTest : EvaluatorTestBase() {
    private val testUnixTime = 1234567890
    
    @Test
    fun `from_unixtime 0 args`() =
        checkInputThrowingEvaluationException(
            "from_unixtime()",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.EXPECTED_ARITY_MIN to 1,
                  Property.EXPECTED_ARITY_MAX to 1))

    @Test
    fun `from_unixtime 2 args`() =
        checkInputThrowingEvaluationException(
            "from_unixtime($testUnixTime, $testUnixTime)",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.EXPECTED_ARITY_MIN to 1,
                  Property.EXPECTED_ARITY_MAX to 1))

    @Test
    fun `from_unixtime 3 args`() =
        checkInputThrowingEvaluationException(
            "from_unixtime($testUnixTime, $testUnixTime, $testUnixTime)",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.EXPECTED_ARITY_MIN to 1,
                  Property.EXPECTED_ARITY_MAX to 1))


    class FromUnixTimeTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // negative unix epochs output null
            FromUnixTimeTestCase("from_unixtime(-1)", "null"),
            FromUnixTimeTestCase("from_unixtime(-0.1)", "null"),
            // non-negative cases outputting a timestamp
            FromUnixTimeTestCase("from_unixtime(0)", "1970-01-01T00:00:00.000-00:00"),
            FromUnixTimeTestCase("from_unixtime(0.001)", "1970-01-01T00:00:00.001-00:00"),
            FromUnixTimeTestCase("from_unixtime(0.01)", "1970-01-01T00:00:00.01-00:00"),
            FromUnixTimeTestCase("from_unixtime(0.1)", "1970-01-01T00:00:00.1-00:00"),
            FromUnixTimeTestCase("from_unixtime(1)", "1970-01-01T00:00:01-00:00"),
            FromUnixTimeTestCase("from_unixtime(1577836800)", "2020-01-01T00:00:00-00:00")
        )
    }
    @ParameterizedTest
    @ArgumentsSource(FromUnixTimeTests::class)
    fun runNoArgTests(tc: FromUnixTimeTestCase) = assertEval(tc.unixTimestamp, tc.expected)
}
