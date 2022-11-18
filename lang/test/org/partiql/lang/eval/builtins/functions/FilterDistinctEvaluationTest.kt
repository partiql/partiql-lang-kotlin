package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to

class FilterDistinctEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ToStringPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(query = testCase.source, expectedResult = testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

    // We rely on the built-in [DEFAULT_COMPARATOR] for the actual definition of equality, which is not being tested
    // here.
    class ToStringPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(

            // These three tests ensure we can accept lists, bags, s-expressions and structs
            ExprFunctionTestCase("filter_distinct([0, 0, 1])", "<<0, 1>>"), // list
            ExprFunctionTestCase("filter_distinct(<<0, 0, 1>>)", "<<0, 1>>"), // bag
            ExprFunctionTestCase("filter_distinct(SEXP(0, 0, 1))", "<<0, 1>>"), // s-exp
            ExprFunctionTestCase("filter_distinct({'a': 0, 'b': 0, 'c': 1})", "<<0, 1>>"), // struct

            // Some "smoke tests" to ensure the basic plumbing is working right.
            ExprFunctionTestCase("filter_distinct(['foo', 'foo', 1, 1, `symbol`, `symbol`])", "<<'foo', 1, `symbol`>>"),
            ExprFunctionTestCase("filter_distinct([{ 'a': 1 }, { 'a': 1 }, { 'a': 1 }])", "<<{ 'a': 1 }>>"),
            ExprFunctionTestCase("filter_distinct([[1, 1], [1, 1], [2, 2]])", "<<[1,1], [2, 2]>>"),
        )
    }

    // Error test cases: Invalid arguments
    data class InvalidArgTestCase(
        val source: String,
        val actualArgumentType: String
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun toStringInvalidArgumentTests(testCase: InvalidArgTestCase) = runEvaluatorErrorTestCase(
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
            InvalidArgTestCase("filter_distinct(1)", "INT"),
            InvalidArgTestCase("filter_distinct(1.0)", "DECIMAL"),
            InvalidArgTestCase("filter_distinct('foo')", "STRING"),
            InvalidArgTestCase("filter_distinct(`some_symbol`)", "SYMBOL"),
            InvalidArgTestCase("filter_distinct(`{{ '''a clob''' }}`)", "CLOB"),
        )
    }

    @Test
    fun invalidArityTest() = checkInvalidArity(funcName = "filter_distinct", maxArity = 1, minArity = 1)
}
