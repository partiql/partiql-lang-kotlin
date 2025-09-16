package org.partiql.runner

import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.eval.Statement
import org.partiql.runner.executor.EvalExecutor
import org.partiql.runner.report.ReportGenerator
import org.partiql.runner.schema.TestCase
import org.partiql.runner.test.TestProvider
import org.partiql.runner.test.TestRunner
import org.partiql.spi.value.Datum

/**
 * Conformance test suite for PartiQL Core dataset using the evaluation engine.
 *
 * This class runs the core PartiQL conformance tests against the Kotlin evaluation engine
 * to ensure compliance with the PartiQL specification. It includes both evaluation tests
 * and equivalence tests with configurable skip lists for known issues.
 */
class ConformanceTestEvalCore : ConformanceTestBase<Statement, Datum>() {
    companion object {
        @JvmStatic
        @RegisterExtension
        val reporter = ReportGenerator(DataSet.PartiQLCore)
    }

    private val factory = EvalExecutor.Factory
    override val runner = TestRunner(factory)

    private val skipListForEvaluation: Set<Pair<String, CompileType>> = getSkipList("/config/eval/skip-eval-core.txt")
    private val skipListForEquivalence: Set<Pair<String, CompileType>> = getSkipList("/config/eval/skip-eval-equiv-core.txt")

    /**
     * Tests the eval tests with the Kotlin implementation
     * Unit is second.
     * This is not a performance test. This is for stop long-running tests during development process in eval engine.
     * This number can be smaller, but to account for the cold start time and fluctuation of GitHub runner,
     * I decided to make this number a bit larger than needed.
     **/
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
