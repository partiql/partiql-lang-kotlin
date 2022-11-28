package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase

class LowerEvaluationTest : EvaluatorTestBase() {
    @ParameterizedTest
    @ArgumentsSource(LowerPassCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) = runEvaluatorTestCase(
        tc.source,
        expectedResult = tc.expectedLegacyModeResult,
        expectedPermissiveModeResult = tc.expectedPermissiveModeResult
    )

    class LowerPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("lower('')", "''"),
            ExprFunctionTestCase("lower(`A`)", "'a'"),
            ExprFunctionTestCase("lower(`'A'`)", "'a'"),
            ExprFunctionTestCase("lower('A')", "'a'"),
            ExprFunctionTestCase("lower(`\"A\"`)", "'a'"),
            ExprFunctionTestCase("lower('ABCDEF')", "'abcdef'"),
            ExprFunctionTestCase("lower('abcdef')", "'abcdef'"),
            ExprFunctionTestCase("lower(null)", "NULL"),
            ExprFunctionTestCase("lower(missing)", "NULL", "MISSING"),
            ExprFunctionTestCase("lower('123\$%(*&')", "'123\$%(*&'"),
            ExprFunctionTestCase("lower('ȴȵ💩Z💋')", "'ȴȵ💩z💋'"),
            ExprFunctionTestCase("lower('話家身圧費谷料村能計税金')", "'話家身圧費谷料村能計税金'")
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun lowerInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "lower",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL), ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun lowerInvalidArityTest() = checkInvalidArity(
        funcName = "lower",
        maxArity = 1,
        minArity = 1
    )
}
