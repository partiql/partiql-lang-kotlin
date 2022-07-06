package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase

class MathFunctionsTest : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(MathFunctionsPassCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) = runEvaluatorTestCase(
        tc.source,
        expectedResult = tc.expectedLegacyModeResult
    )

    class MathFunctionsPassCases : ArgumentsProviderBase() {

        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("ceil(1)", "1"),
            ExprFunctionTestCase("ceil(1.0)", "1"),
            ExprFunctionTestCase("ceil(`1`)", "1"),
            ExprFunctionTestCase("ceil(1.0e0)", "1"),
            ExprFunctionTestCase("ceil(1.1)", "2"),
            ExprFunctionTestCase("ceil(`1.1`)", "2"),
            ExprFunctionTestCase("ceil(1.1e0)", "2"),
            ExprFunctionTestCase("ceil(-42.8)", "-42"),
            ExprFunctionTestCase("ceiling(1)", "1"),
            ExprFunctionTestCase("ceiling(1.0)", "1"),
            ExprFunctionTestCase("ceiling(`1`)", "1"),
            ExprFunctionTestCase("ceiling(1.0e0)", "1"),
            ExprFunctionTestCase("ceiling(1.1)", "2"),
            ExprFunctionTestCase("ceiling(`1.1`)", "2"),
            ExprFunctionTestCase("ceiling(1.1e0)", "2"),
            ExprFunctionTestCase("ceiling(-42.8)", "-42"),
            ExprFunctionTestCase("ceil(`+inf`)", "+inf"),
            ExprFunctionTestCase("ceil(`-inf`)", "-inf"),
            ExprFunctionTestCase("ceil(`nan`)", "nan"),
            ExprFunctionTestCase("floor(1)", "1"),
            ExprFunctionTestCase("floor(1.0)", "1"),
            ExprFunctionTestCase("floor(`1`)", "1"),
            ExprFunctionTestCase("floor(1.0e0)", "1"),
            ExprFunctionTestCase("floor(1.1)", "1"),
            ExprFunctionTestCase("floor(`1.1`)", "1"),
            ExprFunctionTestCase("floor(1.1e0)", "1"),
            ExprFunctionTestCase("floor(-42.8)", "-43"),
            ExprFunctionTestCase("floor(`+inf`)", "+inf"),
            ExprFunctionTestCase("floor(`-inf`)", "-inf"),
            ExprFunctionTestCase("floor(`nan`)", "nan"),
            ExprFunctionTestCase("ceil(`1.00000000000000001`)", "2"),
            ExprFunctionTestCase("ceil(1.00000000000000001)", "2"),
            ExprFunctionTestCase("floor(`1.9999999999999999`)", "1"),
            ExprFunctionTestCase("floor(1.99999999999999999999)", "1")
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun sizeInvalidArgTypeTest() {
        checkInvalidArgType(
            funcName = "ceil",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        checkInvalidArgType(
            funcName = "ceiling",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        checkInvalidArgType(
            funcName = "floor",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
    }

    // Error test cases: Invalid arity
    @Test
    fun sizeInvalidArityTest() {
        checkInvalidArity("ceil", 1, 1)
        checkInvalidArity("ceiling", 1, 1)
        checkInvalidArity("floor", 1, 1)
    }
}
