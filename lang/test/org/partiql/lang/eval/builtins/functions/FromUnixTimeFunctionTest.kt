package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf

class FromUnixTimeFunctionTest : EvaluatorTestBase() {
    @ParameterizedTest
    @ArgumentsSource(FromUnixTimePassCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) = runEvaluatorTestCase(tc.source, tc.expectedLegacyModeResult)

    class FromUnixTimePassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // negative unix epochs output timestamp before last epoch
            ExprFunctionTestCase("from_unixtime(-1)", "1969-12-31T23:59:59-00:00"),
            ExprFunctionTestCase("from_unixtime(-0.1)", "1969-12-31T23:59:59.9-00:00"),
            ExprFunctionTestCase("from_unixtime(`-1`)", "1969-12-31T23:59:59-00:00"),
            ExprFunctionTestCase("from_unixtime(`-0.1`)", "1969-12-31T23:59:59.9-00:00"),
            // non-negative cases outputting a timestamp after last epoch
            ExprFunctionTestCase("from_unixtime(0)", "1970-01-01T00:00:00.000-00:00"),
            ExprFunctionTestCase("from_unixtime(0.001)", "1970-01-01T00:00:00.001-00:00"),
            ExprFunctionTestCase("from_unixtime(0.01)", "1970-01-01T00:00:00.01-00:00"),
            ExprFunctionTestCase("from_unixtime(0.1)", "1970-01-01T00:00:00.1-00:00"),
            ExprFunctionTestCase("from_unixtime(1)", "1970-01-01T00:00:01-00:00"),
            ExprFunctionTestCase("from_unixtime(1577836800)", "2020-01-01T00:00:00-00:00"),
            ExprFunctionTestCase("from_unixtime(`0`)", "1970-01-01T00:00:00.000-00:00"),
            ExprFunctionTestCase("from_unixtime(`0.001`)", "1970-01-01T00:00:00.001-00:00"),
            ExprFunctionTestCase("from_unixtime(`0.01`)", "1970-01-01T00:00:00.01-00:00"),
            ExprFunctionTestCase("from_unixtime(`0.1`)", "1970-01-01T00:00:00.1-00:00"),
            ExprFunctionTestCase("from_unixtime(`1`)", "1970-01-01T00:00:01-00:00"),
            ExprFunctionTestCase("from_unixtime(`1577836800`)", "2020-01-01T00:00:00-00:00"),
            // Null or missing
            ExprFunctionTestCase("from_unixtime(null)", "null"),
            ExprFunctionTestCase("from_unixtime(missing)", "null"),
        )
    }

    // Invalid arguments
    data class InvalidArgTestCase(
        val query: String,
        val message: String
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun fromUnixTimeInvalidArgumentTests(testCase: InvalidArgTestCase) =
        runEvaluatorErrorTestCase(
            testCase.query,
            ErrorCode.EVALUATOR_GENERIC_EXCEPTION,
            propertyValueMapOf(1, 1),
            null,
            true
        )

    class InvalidArgCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            InvalidArgTestCase(
                "from_unixtime(1577836800000)",
                "Internal error, millis: 1.5778368E+15 is outside of valid the range: from -62135769600000 (0001T), inclusive, to 253402300800000 (10000T) , exclusive"
            ),
            InvalidArgTestCase(
                "from_unixtime(-1577836800000)",
                "Internal error, millis: -1.5778368E+15 is outside of valid the range: from -62135769600000 (0001T), inclusive, to 253402300800000 (10000T) , exclusive"
            )
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun fromUnixTimeInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "from_unixtime",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.DECIMAL, StaticType.INT), ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun fromUnixTimeInvalidArityTest() = checkInvalidArity(
        funcName = "from_unixtime",
        maxArity = 1,
        minArity = 1
    )
}
