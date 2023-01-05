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
            //
            // Ceil/ Floor
            //
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

            //
            // abs
            //

            // positive number, returns itself
            ExprFunctionTestCase("abs(1)", "1"),
            ExprFunctionTestCase("abs(1.0)", "1.0"),
            ExprFunctionTestCase("abs(`1d0`)", "1d0"),
            ExprFunctionTestCase("abs(`1e0`)", "1e0"),
            ExprFunctionTestCase("abs(1.9999999999999999999900)", "1.9999999999999999999900"),

            // negative number, returns the negation of the original number
            ExprFunctionTestCase("abs(-1)", "1"),
            ExprFunctionTestCase("abs(-1.0)", "1.0"),
            ExprFunctionTestCase("abs(`-1d0`)", "1d0"),
            ExprFunctionTestCase("abs(`-1e0`)", "1e0"),
            ExprFunctionTestCase("abs(-1.9999999999999999999900)", "1.9999999999999999999900"),

            // all forms of negative zero
            ExprFunctionTestCase("abs(`-0.`)", "0."),
            ExprFunctionTestCase("abs(`-0d0`)", "0."),
            ExprFunctionTestCase("abs(`-0d-0`)", "0."),
            ExprFunctionTestCase("abs(`-0.0d1`)", "0."),
            // preserve scale
            ExprFunctionTestCase("abs(`-0.0000`)", "0.0000"),
            // special value
            ExprFunctionTestCase("abs(`-inf`)", "+inf"),
            ExprFunctionTestCase("abs(`+inf`)", "+inf"),
            ExprFunctionTestCase("abs(`nan`)", "nan"),

            //
            // sqrt
            //
            ExprFunctionTestCase("sqrt(`2.0`)", "1.4142135623730950488016887242096980786"),
            ExprFunctionTestCase("sqrt(2.0)", "1.4142135623730950488016887242096980786"),
            ExprFunctionTestCase("sqrt(1.0)", "1.0000000000000000000000000000000000000"),
            ExprFunctionTestCase("sqrt(4.0)", "2.0000000000000000000000000000000000000"),
            ExprFunctionTestCase("sqrt(`2.e0`)", kotlin.math.sqrt(2.0).toString() + "e0"),
            ExprFunctionTestCase("sqrt(2)", kotlin.math.sqrt(2.0).toString() + "e0"),
            // special value
            ExprFunctionTestCase("sqrt(`+inf`)", "+inf"),
            ExprFunctionTestCase("sqrt(`nan`)", "nan"),

            // exp
            ExprFunctionTestCase("exp(`2.0`)", "7.3890560989306502272304274605750078132"),
            ExprFunctionTestCase("exp(2.0)", "7.3890560989306502272304274605750078132"),
            ExprFunctionTestCase("exp(1.0)", "2.7182818284590452353602874713526624978"),
            ExprFunctionTestCase("exp(`2.e0`)", kotlin.math.exp(2.0).toString() + "e0"),
            ExprFunctionTestCase("exp(2)", kotlin.math.exp(2.0).toString() + "e0"),
            // special value
            ExprFunctionTestCase("exp(`-inf`)", "0e0"),
            ExprFunctionTestCase("exp(`+inf`)", "+inf"),
            ExprFunctionTestCase("exp(`nan`)", "nan"),

            // ln
            ExprFunctionTestCase("ln(`2.0`)", "0.69314718055994530941723212145817656808"),
            ExprFunctionTestCase("ln(2.0)", "0.69314718055994530941723212145817656808"),
            ExprFunctionTestCase("ln(1.0)", "0.0000000000000000000000000000000000000"),
            ExprFunctionTestCase("ln(`2.e0`)", kotlin.math.ln(2.0).toString() + "e0"),
            ExprFunctionTestCase("ln(2)", kotlin.math.ln(2.0).toString() + "e0"),
            // special value
            ExprFunctionTestCase("ln(`+inf`)", "+inf"),
            ExprFunctionTestCase("ln(`nan`)", "nan"),

            // Power
            ExprFunctionTestCase("pow(1, 2)", "1e0"),
            ExprFunctionTestCase("pow(2, 2)", "4e0"),
            ExprFunctionTestCase("pow(`1e0`, 2)", "1e0"),
            ExprFunctionTestCase("pow(`2e0`, 2)", "4e0"),
            ExprFunctionTestCase("pow(2.0, 2)", "4.0000000000000000000000000000000000000"),
            // special value
            ExprFunctionTestCase("pow(`+inf`, 0)", "1e0"),
            ExprFunctionTestCase("pow(`-inf`, 0)", "1e0"),
            ExprFunctionTestCase("pow(`nan`, 0)", "1e0"),
            ExprFunctionTestCase("pow(`+inf`, 0.0)", "1e0"),
            ExprFunctionTestCase("pow(`-inf`, 0.0)", "1e0"),
            ExprFunctionTestCase("pow(`nan`, 0.0)", "1e0"),
            ExprFunctionTestCase("pow(`+inf`, 1)", "+inf"),
            ExprFunctionTestCase("pow(`-inf`, 1)", "-inf"),
            ExprFunctionTestCase("pow(`nan`, 1)", "nan"),
            ExprFunctionTestCase("pow(`+inf`, 2)", "+inf"),
            ExprFunctionTestCase("pow(`-inf`, 2)", "+inf"),
            ExprFunctionTestCase("pow(`nan`, 2)", "nan"),

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
        checkInvalidArgType(
            funcName = "abs",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        checkInvalidArgType(
            funcName = "sqrt",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        checkInvalidArgType(
            funcName = "exp",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        checkInvalidArgType(
            funcName = "ln",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        checkInvalidArgType(
            funcName = "pow",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ","),
                Argument(2, StaticType.NUMERIC, ")")
            )
        )
    }

    // Error test cases: Invalid arity
    @Test
    fun sizeInvalidArityTest() {
        checkInvalidArity("ceil", 1, 1)
        checkInvalidArity("ceiling", 1, 1)
        checkInvalidArity("floor", 1, 1)
        checkInvalidArity("abs", 1, 1)
    }

    @ParameterizedTest
    @ArgumentsSource(MathFunctionErrorTest::class)
    fun runErrorTests(tc: EvaluatorErrorTestCase) = runEvaluatorErrorTestCase(
        tc.query,
        expectedErrorCode = tc.expectedErrorCode,
        expectedPermissiveModeResult = tc.expectedPermissiveModeResult
    )

    class MathFunctionErrorTest : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // Ceiling / Floor
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
            EvaluatorErrorTestCase(
                query = "abs($MIN_INT8-1)",
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
            EvaluatorErrorTestCase(
                query = "ABS(${MAX_INT8}1)",
                expectedErrorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
            ),
            EvaluatorErrorTestCase(
                query = "ABS(${MIN_INT8}1)",
                expectedErrorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
            ),
            // edge case, overflow caused by function evulation
            EvaluatorErrorTestCase(
                query = "ceil($MAX_INT8.1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING",
            ),
            EvaluatorErrorTestCase(
                query = "floor($MIN_INT8.1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING",
            ),

            // ABS
            EvaluatorErrorTestCase(
                query = "abs($MIN_INT8)",
                expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                expectedPermissiveModeResult = "MISSING",
            ),

            // Sqrt
            EvaluatorErrorTestCase(
                query = "sqrt(-1)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),

            // ln
            EvaluatorErrorTestCase(
                query = "ln(-1)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),
            EvaluatorErrorTestCase(
                query = "ln(0)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),
            EvaluatorErrorTestCase(
                query = "ln(0.0)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),
            EvaluatorErrorTestCase(
                query = "ln(-1.0)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),
            EvaluatorErrorTestCase(
                query = "ln(-0.0)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),
            EvaluatorErrorTestCase(
                query = "ln(`-inf`)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),

            // Pow
            EvaluatorErrorTestCase(
                query = "pow(-1.0, 0.1)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),

            EvaluatorErrorTestCase(
                query = "pow(-1.0, `1e-1`)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),

            EvaluatorErrorTestCase(
                query = "pow(`-1e0`, `1e-1`)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),

            EvaluatorErrorTestCase(
                query = "pow(`-1e0`, `0.1`)",
                expectedErrorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                expectedPermissiveModeResult = "MISSING",
            ),

        )
    }
}
