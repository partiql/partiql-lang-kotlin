package org.partiql.lang.eval.builtins

import com.amazon.ion.Timestamp
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.util.ArgumentsProviderBase

data class UnixTimestampNoArgTestCase(val numMillis: Long, val expected: String)
data class UnixTimestampOneArgTestCase(val timestamp: String, val expected: String)

class UnixTimestampFunctionTest : EvaluatorTestBase() {
    private val testTimestamp = "`2007-02-23T12:14Z`"

    @Test
    fun `unix_timestamp 2 args`() =
        checkInputThrowingEvaluationException(
            "unix_timestamp($testTimestamp, $testTimestamp)",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.EXPECTED_ARITY_MIN to 0,
                  Property.EXPECTED_ARITY_MAX to 1))

    @Test
    fun `unix_timestamp 3 args`() =
        checkInputThrowingEvaluationException(
            "unix_timestamp($testTimestamp, $testTimestamp, $testTimestamp)",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.EXPECTED_ARITY_MIN to 0,
                  Property.EXPECTED_ARITY_MAX to 1))


    class NoArgsTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // unix_timestamp no args, now = 0
            UnixTimestampNoArgTestCase(numMillis = 0, expected = "0"),
            // nix_timestamp no args, now = 1ms
            UnixTimestampNoArgTestCase(numMillis = 1, expected = "0"),
            // unix_timestamp no args, now = 999ms
            UnixTimestampNoArgTestCase(numMillis = 999, expected = "0"),
            // unix_timestamp no args, now = 1s
            UnixTimestampNoArgTestCase(numMillis = 1000, expected = "1"),
            // unix_timestamp no args, now = 1001ms
            UnixTimestampNoArgTestCase(numMillis = 1001, expected = "1")
        )
    }
    @ParameterizedTest
    @ArgumentsSource(NoArgsTests::class)
    fun runNoArgTests(tc: UnixTimestampNoArgTestCase) =
        assertEval(
            "unix_timestamp()",
            tc.expected,
            session = EvaluationSession.build { now(Timestamp.forMillis(tc.numMillis, 0)) })


    class OneArgTests : ArgumentsProviderBase() {
        private val epoch2020 = "1577836800"
        private val epoch2020Decimal = "1577836800."

        override fun getParameters(): List<Any> = listOf(
            // time before the last epoch
            UnixTimestampOneArgTestCase("unix_timestamp(`1969T`)", "0"),
            UnixTimestampOneArgTestCase("unix_timestamp(`1969-12-31T23:59:59.999Z`)", "0"),
            // exactly the last epoch
            UnixTimestampOneArgTestCase("unix_timestamp(`1970T`)", "0"),
            UnixTimestampOneArgTestCase("unix_timestamp(`1970-01-01T00:00:00.000Z`)", "0."),
            // whole number unix epoch
            UnixTimestampOneArgTestCase("unix_timestamp(`2020T`)", epoch2020),
            UnixTimestampOneArgTestCase("unix_timestamp(`2020-01T`)", epoch2020),
            UnixTimestampOneArgTestCase("unix_timestamp(`2020-01-01T`)", epoch2020),
            UnixTimestampOneArgTestCase("unix_timestamp(`2020-01-01T00:00Z`)", epoch2020),
            UnixTimestampOneArgTestCase("unix_timestamp(`2020-01-01T00:00:00Z`)", epoch2020),
            // decimal unix epoch
            UnixTimestampOneArgTestCase("unix_timestamp(`2020-01-01T00:00:00.0Z`)", epoch2020Decimal),
            UnixTimestampOneArgTestCase("unix_timestamp(`2020-01-01T00:00:00.00Z`)", epoch2020Decimal),
            UnixTimestampOneArgTestCase("unix_timestamp(`2020-01-01T00:00:00.000Z`)", epoch2020Decimal),
            UnixTimestampOneArgTestCase("unix_timestamp(`2020-01-01T00:00:00.100Z`)", "1577836800.1")
        )
    }
    @ParameterizedTest
    @ArgumentsSource(OneArgTests::class)
    fun runOneArgTests(tc: UnixTimestampOneArgTestCase) = assertEval(tc.timestamp, tc.expected)
}
