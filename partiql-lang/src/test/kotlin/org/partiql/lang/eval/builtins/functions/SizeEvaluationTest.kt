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

class SizeEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(SizePassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, expectedResult = testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

    class SizePassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // SQL standard builtin name
            ExprFunctionTestCase("cardinality(<<1, 2, 3>>)", "3"),
            ExprFunctionTestCase("cardinality(<<>>)", "0"),
            ExprFunctionTestCase("cardinality(sexp(1,2))", "2"),
            ExprFunctionTestCase("cardinality(`(1 2)`)", "2"),
            ExprFunctionTestCase("cardinality(sexp())", "0"),
            ExprFunctionTestCase("cardinality(`()`)", "0"),
            ExprFunctionTestCase("cardinality([1])", "1"),
            ExprFunctionTestCase("cardinality(`[1]`)", "1"),
            ExprFunctionTestCase("cardinality([])", "0"),
            ExprFunctionTestCase("cardinality(`[]`)", "0"),
            ExprFunctionTestCase("cardinality({ `a`: 1, `b`: 2, `c`: 3 })", "3"),
            ExprFunctionTestCase("cardinality(`{ a: 1, b: 2, c: 3 }`)", "3"),
            ExprFunctionTestCase("cardinality({})", "0"),
            ExprFunctionTestCase("cardinality(`{}`)", "0"),
            ExprFunctionTestCase("cardinality(null)", "null"),
            ExprFunctionTestCase("cardinality(missing)", "null", "$MISSING_ANNOTATION::null"),
            // PartiQL alternative builtin name
            ExprFunctionTestCase("size(<<1, 2, 3>>)", "3"),
            ExprFunctionTestCase("size(<<>>)", "0"),
            ExprFunctionTestCase("size(sexp(1,2))", "2"),
            ExprFunctionTestCase("size(`(1 2)`)", "2"),
            ExprFunctionTestCase("size(sexp())", "0"),
            ExprFunctionTestCase("size(`()`)", "0"),
            ExprFunctionTestCase("size([1])", "1"),
            ExprFunctionTestCase("size(`[1]`)", "1"),
            ExprFunctionTestCase("size([])", "0"),
            ExprFunctionTestCase("size(`[]`)", "0"),
            ExprFunctionTestCase("size({ `a`: 1, `b`: 2, `c`: 3 })", "3"),
            ExprFunctionTestCase("size(`{ a: 1, b: 2, c: 3 }`)", "3"),
            ExprFunctionTestCase("size({})", "0"),
            ExprFunctionTestCase("size(`{}`)", "0"),
            ExprFunctionTestCase("size(null)", "null"),
            ExprFunctionTestCase("size(missing)", "null", "$MISSING_ANNOTATION::null"),
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun sizeInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "size",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.LIST, StaticType.BAG, StaticType.STRUCT, StaticType.SEXP), ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun sizeInvalidArityTest() = checkInvalidArity(
        funcName = "size",
        maxArity = 1,
        minArity = 1
    )
}
