package org.partiql.runner

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.CompileOptions
import org.partiql.runner.executor.EvalExecutor
import org.partiql.runner.report.ReportGenerator
import org.partiql.runner.schema.TestCase
import org.partiql.runner.skip.LANG_KOTLIN_EVAL_EQUIV_FAIL_LIST
import org.partiql.runner.test.TestProvider
import org.partiql.runner.test.TestRunner

@ExtendWith(ReportGenerator::class)
@Disabled
class ConformanceTestEval {

    private val factory = EvalExecutor.Factory
    private val runner = TestRunner(factory)

    // Tests the eval tests with the Kotlin implementation
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Eval::class)
    fun validatePartiQLEvalTestData(tc: TestCase) {
        // val skip = LANG_KOTLIN_EVAL_FAIL_LIST
        val skip = emptyList<Pair<String, CompileOptions>>()
        when (tc) {
            is TestCase.Eval -> runner.test(tc, skip)
            else -> error("Unsupported test case category")
        }
    }

    // Tests the eval equivalence tests with the Kotlin implementation
    @Disabled
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Equiv::class)
    fun validatePartiQLEvalEquivTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Equiv -> runner.test(tc, LANG_KOTLIN_EVAL_EQUIV_FAIL_LIST)
            else -> error("Unsupported test case category")
        }
    }
}
