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

class ExistsEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ExistsPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, testCase.expectedLegacyModeResult)

    class ExistsPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("exists(<<1, 2, 3>>)", "true"),
            ExprFunctionTestCase("exists(<<>>)", "false"),
            ExprFunctionTestCase("exists(sexp(1,2,3))", "true"),
            ExprFunctionTestCase("exists(`(1 2 3)`)", "true"),
            ExprFunctionTestCase("exists(sexp())", "false"),
            ExprFunctionTestCase("exists(`()`)", "false"),
            ExprFunctionTestCase("exists([1, 2, 3])", "true"),
            ExprFunctionTestCase("exists(`[1, 2, 3]`)", "true"),
            ExprFunctionTestCase("exists([])", "false"),
            ExprFunctionTestCase("exists(`[]`)", "false"),
            ExprFunctionTestCase("exists({ `a`: 1, `b`: 2, `c`: 3 })", "true"),
            ExprFunctionTestCase("exists(`{ a: 1, b: 2, c: 3 }`)", "true"),
            ExprFunctionTestCase("exists({})", "false"),
            ExprFunctionTestCase("exists(`{}`)", "false")
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun existsInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "exists",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.SEXP, StaticType.LIST, StaticType.BAG, StaticType.STRUCT), ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun existsInvalidArityTest() = checkInvalidArity(
        funcName = "exists",
        maxArity = 1,
        minArity = 1
    )
}
