package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.eval.evaluatortestframework.EvaluatorErrorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase

// constant that can be handy in testing
private val MIN_INT2 = Short.MIN_VALUE
private val MAX_INT2 = Short.MAX_VALUE
private val MIN_INT4 = Int.MIN_VALUE
private val MAX_INT4 = Int.MAX_VALUE
private val MIN_INT8 = Long.MIN_VALUE
private val MAX_INT8 = Long.MAX_VALUE

class MathFunctionsTest : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(MathFunctionsPassCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) = runEvaluatorTestCase(
        tc.source,
        expectedResult = tc.expectedLegacyModeResult
    )

    class MathFunctionsPassCases : ArgumentsProviderBase() {

        override fun getParameters(): List<Any> = listOf(
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
            // test case for literal larger than 64 bits
            ExprFunctionTestCase("ceil(`1.00000000000000001`)", "2"),
            ExprFunctionTestCase("ceil(1.00000000000000001)", "2"),
            ExprFunctionTestCase("floor(`1.9999999999999999`)", "1"),
            ExprFunctionTestCase("floor(1.99999999999999999999)", "1"),

        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun sizeInvalidArgTypeTest() {
        checkInvalidArgType(
            funcName = "ceil",
            args = listOf(
                Argument(1, StaticType.NUMBER, ")")
            )
        )
        checkInvalidArgType(
            funcName = "ceiling",
            args = listOf(
                Argument(1, StaticType.NUMBER, ")")
            )
        )
        checkInvalidArgType(
            funcName = "floor",
            args = listOf(
                Argument(1, StaticType.NUMBER, ")")
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

    @ParameterizedTest
    @ArgumentsSource(MathFunctionOverflowTest::class)
    fun runOverflowTests(tc: EvaluatorErrorTestCase) = runEvaluatorErrorTestCase(
        tc.query,
        expectedErrorCode = tc.expectedErrorCode,
        expectedPermissiveModeResult = tc.expectedPermissiveModeResult
    )

    class MathFunctionOverflowTest : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // overflow caused by expression evaluation inside the function
            EvaluatorErrorTestCase(
                query = "floor($MAX_INT8+1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING"
            ),
            EvaluatorErrorTestCase(
                query = "floor($MIN_INT8-1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING"
            ),
            EvaluatorErrorTestCase(
                query = "ceil($MAX_INT8+1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING"
            ),
            EvaluatorErrorTestCase(
                query = "ceil($MIN_INT8-1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING"
            ),
            // overflow caused by argument
            EvaluatorErrorTestCase(
                query = "floor(${MAX_INT8}1)",
                expectedErrorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
            ),
            EvaluatorErrorTestCase(
                query = "floor(${MIN_INT8}1)",
                expectedErrorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
            ),
            EvaluatorErrorTestCase(
                query = "CEIL(${MAX_INT8}1)",
                expectedErrorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
            ),
            EvaluatorErrorTestCase(
                query = "CEIL(${MIN_INT8}1)",
                expectedErrorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
            ),
            // edge case, overflow caused by function evulation
            EvaluatorErrorTestCase(
                query = "ceil($MAX_INT8.1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING",
                targetPipeline = EvaluatorTestTarget.ALL_PIPELINES
            ),
            EvaluatorErrorTestCase(
                query = "floor($MIN_INT8.1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING",
                targetPipeline = EvaluatorTestTarget.ALL_PIPELINES
            ),
        )
    }
}
