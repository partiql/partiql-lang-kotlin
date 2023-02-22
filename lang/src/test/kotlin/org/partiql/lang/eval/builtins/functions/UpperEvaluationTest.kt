package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.types.StaticType

class UpperEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(UpperPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, expectedResult = testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

    class UpperPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("upper('')", "\"\""),
            ExprFunctionTestCase("upper(`a`)", "\"A\""),
            ExprFunctionTestCase("upper(`'a'`)", "\"A\""),
            ExprFunctionTestCase("upper('a')", "\"A\""),
            ExprFunctionTestCase("upper(`\"a\"`)", "\"A\""),
            ExprFunctionTestCase("upper('abcdef')", "\"ABCDEF\""),
            ExprFunctionTestCase("upper('ABCDEF')", "\"ABCDEF\""),
            ExprFunctionTestCase("upper(null)", "null"),
            ExprFunctionTestCase("upper(missing)", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("upper('123\$%(*&')", "\"123\$%(*&\""),
            ExprFunctionTestCase("upper('ȴȵ💩z💋')", "\"ȴȵ💩Z💋\""),
            ExprFunctionTestCase("upper('話家身圧費谷料村能計税金')", "\"話家身圧費谷料村能計税金\"")
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun upperInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "upper",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL), ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun upperInvalidArityTest() = checkInvalidArity(
        funcName = "upper",
        maxArity = 1,
        minArity = 1
    )
}
