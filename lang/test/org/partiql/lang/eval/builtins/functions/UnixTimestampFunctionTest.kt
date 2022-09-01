package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.buildStandardPluginWithNow
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.ots_work.stscore.ScalarTypeSystem
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase

class UnixTimestampFunctionTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(UnixTimestampPassCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) =
        runEvaluatorTestCase(
            query = tc.source,
            scalarTypeSystem = tc.scalarTypeSystem,
            expectedResult = tc.expectedLegacyModeResult,
            expectedPermissiveModeResult = tc.expectedPermissiveModeResult,
        )

    class UnixTimestampPassCases : ArgumentsProviderBase() {
        private val epoch2020 = "1577836800"
        private val epoch2020Decimal = "1577836800."

        override fun getParameters(): List<Any> = listOf(
            // No args
            ExprFunctionTestCase("unix_timestamp()", "0", scalarTypeSystem = ScalarTypeSystem(buildStandardPluginWithNow(0, 0))), // now = 0
            ExprFunctionTestCase("unix_timestamp()", "0", scalarTypeSystem = ScalarTypeSystem(buildStandardPluginWithNow(1, 0))), // now = 1ms
            ExprFunctionTestCase("unix_timestamp()", "0", scalarTypeSystem = ScalarTypeSystem(buildStandardPluginWithNow(999, 0))), // now = 999ms
            ExprFunctionTestCase("unix_timestamp()", "1", scalarTypeSystem = ScalarTypeSystem(buildStandardPluginWithNow(1000, 0))), // now = 1s
            ExprFunctionTestCase("unix_timestamp()", "1", scalarTypeSystem = ScalarTypeSystem(buildStandardPluginWithNow(1001, 0))), // now = 1001ms
            // time before the last epoch
            ExprFunctionTestCase("unix_timestamp(`1969T`)", "-31536000"),
            ExprFunctionTestCase("unix_timestamp(`1969-12-31T23:59:59.999Z`)", "-0.001"),
            // exactly the last epoch
            ExprFunctionTestCase("unix_timestamp(`1970T`)", "0"),
            ExprFunctionTestCase("unix_timestamp(`1970-01-01T00:00:00.000Z`)", "0."),
            // whole number unix epoch
            ExprFunctionTestCase("unix_timestamp(`2020T`)", epoch2020),
            ExprFunctionTestCase("unix_timestamp(`2020-01T`)", epoch2020),
            ExprFunctionTestCase("unix_timestamp(`2020-01-01T`)", epoch2020),
            ExprFunctionTestCase("unix_timestamp(`2020-01-01T00:00Z`)", epoch2020),
            ExprFunctionTestCase("unix_timestamp(`2020-01-01T00:00:00Z`)", epoch2020),
            // decimal unix epoch
            ExprFunctionTestCase("unix_timestamp(`2020-01-01T00:00:00.0Z`)", epoch2020Decimal),
            ExprFunctionTestCase("unix_timestamp(`2020-01-01T00:00:00.00Z`)", epoch2020Decimal),
            ExprFunctionTestCase("unix_timestamp(`2020-01-01T00:00:00.000Z`)", epoch2020Decimal),
            ExprFunctionTestCase("unix_timestamp(`2020-01-01T00:00:00.100Z`)", "1577836800.1")
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun unixTimestampInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "unix_timestamp",
        args = listOf(
            Argument(1, StaticType.TIMESTAMP, ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun unixTimestampInvalidArityTest() = checkInvalidArity(
        funcName = "unix_timestamp",
        minArity = 0,
        maxArity = 1
    )
}
