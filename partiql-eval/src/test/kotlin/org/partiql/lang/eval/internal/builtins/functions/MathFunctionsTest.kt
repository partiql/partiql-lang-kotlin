package org.partiql.lang.eval.internal.builtins.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorErrorTestCase
import org.partiql.lang.eval.internal.builtins.Argument
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.types.StaticType

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
    fun runPassTests(tc: org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase) = runEvaluatorTestCase(
        tc.source,
        expectedResult = tc.expectedLegacyModeResult
    )

    class MathFunctionsPassCases : ArgumentsProviderBase() {

        override fun getParameters(): List<Any> = listOf(
            //
            // Ceil/ Floor
            //
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(1.0)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(`1`)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(1.0e0)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(1.1)", "2"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(`1.1`)", "2"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(1.1e0)", "2"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(-42.8)", "-42"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceiling(1)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceiling(1.0)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceiling(`1`)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceiling(1.0e0)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceiling(1.1)", "2"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceiling(`1.1`)", "2"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceiling(1.1e0)", "2"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceiling(-42.8)", "-42"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(`+inf`)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(`-inf`)", "-inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(`nan`)", "nan"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(1)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(1.0)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(`1`)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(1.0e0)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(1.1)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(`1.1`)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(1.1e0)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(-42.8)", "-43"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(`+inf`)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(`-inf`)", "-inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(`nan`)", "nan"),
            // test case for literal larger than 64 bits
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(`1.00000000000000001`)", "2"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ceil(1.00000000000000001)", "2"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(`1.9999999999999999`)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("floor(1.99999999999999999999)", "1"),

            //
            // MOD
            //
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("mod(1, 1)", "0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("mod(10, 1)", "0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("mod(17, 1)", "0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("mod(-17, 4)", "-1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("mod(17, -4)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("mod(10, 3)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("mod(17, 1)", "0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("mod(17, 3)", "2"),

            //
            // abs
            //

            // positive number, returns itself
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(1)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(1.0)", "1.0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`1d0`)", "1d0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`1e0`)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "abs(1.9999999999999999999900)",
                "1.9999999999999999999900"
            ),

            // negative number, returns the negation of the original number
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(-1)", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(-1.0)", "1.0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`-1d0`)", "1d0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`-1e0`)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "abs(-1.9999999999999999999900)",
                "1.9999999999999999999900"
            ),

            // all forms of negative zero
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`-0.`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`-0d0`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`-0d-0`)", "0."),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`-0.0d1`)", "0."),
            // preserve scale
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`-0.0000`)", "0.0000"),
            // special value
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`-inf`)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`+inf`)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("abs(`nan`)", "nan"),

            //
            // sqrt
            //
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "sqrt(`2.0`)",
                "1.4142135623730950488016887242096980786"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "sqrt(2.0)",
                "1.4142135623730950488016887242096980786"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "sqrt(1.0)",
                "1.0000000000000000000000000000000000000"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "sqrt(4.0)",
                "2.0000000000000000000000000000000000000"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "sqrt(`2.e0`)",
                kotlin.math.sqrt(2.0).toString() + "e0"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "sqrt(2)",
                kotlin.math.sqrt(2.0).toString() + "e0"
            ),
            // special value
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("sqrt(`+inf`)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("sqrt(`nan`)", "nan"),

            // exp
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "exp(`2.0`)",
                "7.3890560989306502272304274605750078132"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "exp(2.0)",
                "7.3890560989306502272304274605750078132"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "exp(1.0)",
                "2.7182818284590452353602874713526624978"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "exp(`2.e0`)",
                kotlin.math.exp(2.0).toString() + "e0"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "exp(2)",
                kotlin.math.exp(2.0).toString() + "e0"
            ),
            // special value
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("exp(`-inf`)", "0e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("exp(`+inf`)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("exp(`nan`)", "nan"),

            // ln
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "ln(`2.0`)",
                "0.69314718055994530941723212145817656808"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "ln(2.0)",
                "0.69314718055994530941723212145817656808"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "ln(1.0)",
                "0.0000000000000000000000000000000000000"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "ln(`2.e0`)",
                kotlin.math.ln(2.0).toString() + "e0"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "ln(2)",
                kotlin.math.ln(2.0).toString() + "e0"
            ),
            // special value
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ln(`+inf`)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("ln(`nan`)", "nan"),

            // Power
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(1, 2)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(2, 2)", "4e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`1e0`, 2)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`2e0`, 2)", "4e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "pow(2.0, 2)",
                "4.0000000000000000000000000000000000000"
            ),
            // special value
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`+inf`, 0)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`-inf`, 0)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`nan`, 0)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`+inf`, 0.0)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`-inf`, 0.0)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`nan`, 0.0)", "1e0"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`+inf`, 1)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`-inf`, 1)", "-inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`nan`, 1)", "nan"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`+inf`, 2)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`-inf`, 2)", "+inf"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("pow(`nan`, 2)", "nan"),

        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun sizeInvalidArgTypeTest() {
        org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
            funcName = "ceil",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
            funcName = "ceiling",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
            funcName = "floor",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
            funcName = "abs",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
            funcName = "sqrt",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
            funcName = "exp",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
            funcName = "ln",
            args = listOf(
                Argument(1, StaticType.NUMERIC, ")")
            )
        )
        org.partiql.lang.eval.internal.builtins.checkInvalidArgType(
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
        org.partiql.lang.eval.internal.builtins.checkInvalidArity("ceil", 1, 1)
        org.partiql.lang.eval.internal.builtins.checkInvalidArity("ceiling", 1, 1)
        org.partiql.lang.eval.internal.builtins.checkInvalidArity("floor", 1, 1)
        org.partiql.lang.eval.internal.builtins.checkInvalidArity("abs", 1, 1)
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
