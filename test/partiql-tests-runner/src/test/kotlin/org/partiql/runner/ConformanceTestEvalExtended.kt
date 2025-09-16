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
 * Conformance test suite for PartiQL Extended dataset using the evaluation engine.
 * 
 * This class runs the extended PartiQL conformance tests against the Kotlin evaluation engine.
 * The extended dataset includes additional test cases beyond the PartiQL specification.
 *
 */
class ConformanceTestEvalExtended : ConformanceTestBase<Statement, Datum>() {
    companion object {
        @JvmStatic
        @RegisterExtension
        val reporter = ReportGenerator(DataSet.PartiQLExtended)
    }

    private val factory = EvalExecutor.Factory
    override val runner = TestRunner(factory)

    private val skipListForEvaluationExtended: Set<Pair<String, CompileType>> = getSkipList("/config/eval/skip-eval-extended.txt")

    // Tests the eval tests data from extended dataset with the Kotlin implementation
    @Timeout(value = 500, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.EvalExtended::class)
    fun validatePartiQLEvalTestDataExtended(tc: TestCase) {
        when (tc) {
            is TestCase.Eval -> runner.test(tc, skipListForEvaluationExtended)
            else -> error("Unsupported test case category")
        }
    }
}
