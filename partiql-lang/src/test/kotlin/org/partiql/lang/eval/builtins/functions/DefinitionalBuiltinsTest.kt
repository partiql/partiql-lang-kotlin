package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorErrorTestCase
import org.partiql.lang.util.ArgumentsProviderBase

class DefinitionalBuiltinsTest : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(PassCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) = runEvaluatorTestCase(
        tc.source,
        expectedResult = tc.expectedLegacyModeResult
    )

    class PassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("coll_to_scalar( << {'a': 1} >> )", "1"),
            ExprFunctionTestCase("coll_to_scalar( [ {'a': 1} ] )", "1"),
            ExprFunctionTestCase("coll_to_scalar(<< {'a': {'aa': 11, 'bb': 22}} >> )", "{'aa': 11, 'bb': 22}"),
            ExprFunctionTestCase("coll_to_scalar( << {'a': []} >> )", "[]"),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ErrorCases::class)
    fun runErrorTests(tc: EvaluatorErrorTestCase) = runEvaluatorErrorTestCase(
        tc.query,
        expectedErrorCode = tc.expectedErrorCode,
        expectedPermissiveModeResult = tc.expectedPermissiveModeResult
    )

    class ErrorCases : ArgumentsProviderBase() {
        fun errorcase(query: String) =
            EvaluatorErrorTestCase(
                query = query,
                expectedErrorCode = ErrorCode.EVALUATOR_NON_SINGLETON_COLLECTION,
                expectedPermissiveModeResult = "MISSING",
            )
        override fun getParameters(): List<Any> = listOf(
            errorcase("coll_to_scalar( 1 )"),
            errorcase("coll_to_scalar( {'a': 1} )"),
            errorcase("coll_to_scalar( << >> )"),
            errorcase("coll_to_scalar( << {'a': 1}, {'a': 2} >> )"),
            errorcase("coll_to_scalar( [ {'a': 1}, 2 ] )"),
            errorcase("coll_to_scalar( [ 1 ] )"),
            errorcase("coll_to_scalar( [ <<>> ] )"),
            errorcase("coll_to_scalar( [ {'a': 1, 'b': 2} ] )"),
            errorcase("coll_to_scalar(  << {} >> )"),
        )
    }
}
