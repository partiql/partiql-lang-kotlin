package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.buildSessionWithNow
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.util.ArgumentsProviderBase

class UtcNowEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(UtcNowPassCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) =
        runEvaluatorTestCase(tc.source, tc.session, tc.expectedLegacyModeResult)

    class UtcNowPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("utcnow()", "1970-01-01T00:00:00.000Z", session = buildSessionWithNow(0, 0)),
            ExprFunctionTestCase("utcnow()", "1970-01-01T00:00:01.000Z", session = buildSessionWithNow(1_000, 0)),
            ExprFunctionTestCase("utcnow()", "1970-01-01T00:05:00.000Z", session = buildSessionWithNow(5L * 60 * 1_000, 1)) // 1970-01-01T00:05:01.000+00:01
        )
    }

    // Error test cases: Invalid arity
    @Test
    fun utcNowInvalidArityTest() = checkInvalidArity(
        funcName = "utcnow",
        maxArity = 0,
        minArity = 0
    )
}
