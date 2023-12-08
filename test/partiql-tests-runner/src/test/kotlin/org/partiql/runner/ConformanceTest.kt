package org.partiql.runner

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.runner.schema.TestCase
import org.partiql.runner.skip.LANG_KOTLIN_EVAL_EQUIV_FAIL_LIST
import org.partiql.runner.skip.LANG_KOTLIN_EVAL_FAIL_LIST
import org.partiql.runner.test.TestProvider
import org.partiql.runner.test.TestRunner
import org.partiql.runner.test.executor.LegacyExecutor

/**
 * Runs the conformance tests with an expected list of failing tests. Ensures that tests not in the failing list
 * succeed with the expected result. Ensures that tests included in the failing list fail.
 *
 * These tests are included in the normal test/building.
 * Update May 2023: Now excluded from the normal build, because the fail lists are out of date.
 * TODO: Come up with a low-burden method of maintaining fail / exclusion lists.
 */
class ConformanceTest {

    private val factory = LegacyExecutor.Factory
    private val runner = TestRunner(factory)

    // Tests the eval tests with the Kotlin implementation
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Eval::class)
    fun validatePartiQLEvalTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Eval -> runner.test(tc, LANG_KOTLIN_EVAL_FAIL_LIST)
            else -> error("Unsupported test case category")
        }
    }

    // Tests the eval equivalence tests with the Kotlin implementation
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Equiv::class)
    fun validatePartiQLEvalEquivTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Equiv -> runner.test(tc, LANG_KOTLIN_EVAL_EQUIV_FAIL_LIST)
            else -> error("Unsupported test case category")
        }
    }
}
