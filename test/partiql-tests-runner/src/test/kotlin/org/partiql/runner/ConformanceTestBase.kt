package org.partiql.runner

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.runner.schema.TestCase
import org.partiql.runner.test.TestProvider
import org.partiql.runner.test.TestRunner

abstract class ConformanceTestBase<T, V> {
    abstract val runner: TestRunner<T, V>

    // Tests the eval tests with the Kotlin implementation
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Eval::class)
    fun validatePartiQLEvalTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Eval -> runner.test(tc, emptyList())
            else -> error("Unsupported test case category")
        }
    }

    // Tests the eval equivalence tests with the Kotlin implementation
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Equiv::class)
    fun validatePartiQLEvalEquivTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Equiv -> runner.test(tc, emptyList())
            else -> error("Unsupported test case category")
        }
    }
}
