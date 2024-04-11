package org.partiql.runner

import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.CompileOptions
import org.partiql.runner.schema.TestCase
import org.partiql.runner.test.TestProvider
import org.partiql.runner.test.TestRunner

abstract class ConformanceTestBase<T, V> {
    abstract val runner: TestRunner<T, V>
    abstract val skipListForEvaluation: List<Pair<String, CompileOptions>>
    abstract val skipListForEquivalence: List<Pair<String, CompileOptions>>

    // Tests the eval tests with the Kotlin implementation
    // Unit is second.
    // This is not a performance test. This is for stop long-running tests during development process in eval engine.
    // This number can be smaller, but to account for the cold start time and fluctuation of GitHub runner,
    // I decided to make this number a bit larger than needed.
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Eval::class)
    fun validatePartiQLEvalTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Eval -> runner.test(tc, skipListForEvaluation)
            else -> error("Unsupported test case category")
        }
    }

    // Tests the eval equivalence tests with the Kotlin implementation
    @Timeout(value = 500, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Equiv::class)
    fun validatePartiQLEvalEquivTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Equiv -> runner.test(tc, skipListForEquivalence)
            else -> error("Unsupported test case category")
        }
    }
}
