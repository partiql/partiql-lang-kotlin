package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.buildStandardPluginWithNow
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.ots_work.stscore.ScalarTypeSystem
import org.partiql.lang.util.ArgumentsProviderBase

class UtcNowEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(UtcNowPassCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) =
        runEvaluatorTestCase(tc.source, tc.session, tc.expectedLegacyModeResult, scalarTypeSystem = tc.scalarTypeSystem)

    class UtcNowPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("utcnow()", "1970-01-01T00:00:00.000Z", scalarTypeSystem = ScalarTypeSystem(buildStandardPluginWithNow(0, 0))),
            ExprFunctionTestCase("utcnow()", "1970-01-01T00:00:01.000Z", scalarTypeSystem = ScalarTypeSystem(buildStandardPluginWithNow(1_000, 0))),
            ExprFunctionTestCase("utcnow()", "1970-01-01T00:06:00.000+00:01", scalarTypeSystem = ScalarTypeSystem(buildStandardPluginWithNow(5L * 60 * 1_000, 1)))
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
