package org.partiql.runner

import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.runner.report.ReportGenerator
import org.partiql.runner.schema.TestCase
import org.partiql.runner.test.TestProvider
import org.partiql.runner.test.TestRunner
import org.partiql.runner.test.executor.LegacyExecutor

/**
 * Runs the conformance tests without a fail list, so we can document the passing/failing tests in the conformance
 * report.
 *
 * These tests are excluded from normal testing/building unless the `conformanceReport` gradle property is
 * specified (i.e. `gradle test ... -PconformanceReport`)
 */
@ExtendWith(ReportGenerator::class)
class ConformanceTestReport {

    private val factory = LegacyExecutor.Factory
    private val runner = TestRunner(factory)

    // Tests the eval tests with the Kotlin implementation without a fail list
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Eval::class)
    fun validatePartiQLEvalTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Eval -> runner.test(tc, emptyList())
            else -> error("Unsupported test case category")
        }
    }

    // Tests the eval equivalence tests with the Kotlin implementation without a fail list
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Equiv::class)
    fun validatePartiQLEvalEquivTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Equiv -> runner.test(tc, emptyList())
            else -> error("Unsupported test case category")
        }
    }
}
