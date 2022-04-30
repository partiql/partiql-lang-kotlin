package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.DYNAMIC_LOOKUP_FUNCTION_NAME
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to

class DynamicLookupExprFunctionTest : EvaluatorTestBase() {
    val session = mapOf(
        "a" to "{ foo: 42 }",
        "b" to "{ bar: 43 }",
        "foo" to "44"
    )

    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ToStringPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(
            query = testCase.source,
            expectedResult = testCase.expectedLegacyModeResult,
            target = EvaluatorTestTarget.PLANNER_PIPELINE,
            expectedResultFormat = ExpectedResultFormat.ION
        )

    // We rely on the built-in [DEFAULT_COMPARATOR] for the actual definition of equality, which is not being tested
    // here.
    class ToStringPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"('foo', `case_insensitive`, `locals_first`, a, b)", "42"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"('foo', `case_insensitive`, `globals_first`, a, b)", "44"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"('foo', `case_sensitive`, `locals_first`, a, b)", "42"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"('foo', `case_sensitive`, `globals_first`, a, b)", "44")
        )
    }

    // Error test cases: Invalid arguments
    data class InvalidArgTestCase(
        val source: String,
        val actualArgumentType: String
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun invalidArgumentTests(testCase: InvalidArgTestCase) = runEvaluatorErrorTestCase(
        query = testCase.source,
        expectedErrorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
        expectedErrorContext = propertyValueMapOf(
            1, 1,
            Property.FUNCTION_NAME to "filter_distinct",
            Property.EXPECTED_ARGUMENT_TYPES to "BAG, LIST, SEXP, or STRUCT",
            Property.ACTUAL_ARGUMENT_TYPES to testCase.actualArgumentType,
            Property.ARGUMENT_POSITION to 1
        ),
        expectedPermissiveModeResult = "MISSING",
    )

    class InvalidArgCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            InvalidArgTestCase("`$DYNAMIC_LOOKUP_FUNCTION_NAME`(1)", "INT")
        )
    }

    @Test
    fun invalidArityTest() = checkInvalidArity(funcName = "filter_distinct", maxArity = 1, minArity = 1)
}