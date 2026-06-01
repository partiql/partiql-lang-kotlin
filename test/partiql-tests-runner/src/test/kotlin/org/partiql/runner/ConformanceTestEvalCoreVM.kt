package org.partiql.runner

import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.runner.executor.VMEvalExecutor
import org.partiql.runner.executor.VMPreparedPlan
import org.partiql.runner.report.ReportGenerator
import org.partiql.runner.schema.TestCase
import org.partiql.runner.test.TestProvider
import org.partiql.runner.test.TestRunner
import org.partiql.spi.value.Datum

/**
 * Conformance test suite for PartiQL Core dataset using the thread-safe VM path.
 *
 * Uses: PartiQLPlanner.builder().useRefs() → PartiQLCompiler.compile() → PartiQLVM.execute()
 *
 * This mirrors [ConformanceTestEvalCore] but exercises the new thread-safe execution path.
 */
class ConformanceTestEvalCoreVM : ConformanceTestBase<VMPreparedPlan, Datum>() {
    companion object {
        @JvmStatic
        @RegisterExtension
        val reporter = ReportGenerator(DataSet.PartiQLCore)
    }

    private val factory = VMEvalExecutor.Factory
    override val runner = TestRunner(factory)

    private val skipListForEvaluation: Set<Pair<String, CompileType>> = getSkipList("/config/eval/skip-eval-core.txt")
    private val skipListForEquivalence: Set<Pair<String, CompileType>> = getSkipList("/config/eval/skip-eval-equiv-core.txt")

    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Eval::class)
    fun validatePartiQLEvalTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Eval -> runner.test(tc, skipListForEvaluation)
            else -> error("Unsupported test case category")
        }
    }

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
