package org.partiql.lang.eval.internal.builtins.functions

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.eval.framework.testcase.impl.EvaluatorErrorTestCase
import org.partiql.eval.util.ArgumentsProviderBase
import org.partiql.lang.eval.EvaluatorTestBase

class DefinitionalBuiltinsTest : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(PassCases::class)
    fun runPassTests(tc: org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase) = runEvaluatorTestCase(
        tc.source,
        expectedResult = tc.expectedLegacyModeResult
    )

    class PassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("coll_to_scalar( << {'a': 1} >> )", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("coll_to_scalar( [ {'a': 1} ] )", "1"),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase(
                "coll_to_scalar(<< {'a': {'aa': 11, 'bb': 22}} >> )",
                "{'aa': 11, 'bb': 22}"
            ),
            org.partiql.lang.eval.internal.builtins.ExprFunctionTestCase("coll_to_scalar( << {'a': []} >> )", "[]"),
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
