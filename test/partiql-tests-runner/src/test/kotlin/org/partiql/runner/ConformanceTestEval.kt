package org.partiql.runner

import org.junit.jupiter.api.extension.RegisterExtension
import org.partiql.eval.PartiQLResult
import org.partiql.eval.PartiQLStatement
import org.partiql.runner.executor.EvalExecutor
import org.partiql.runner.report.ReportGenerator
import org.partiql.runner.test.TestRunner

class ConformanceTestEval : ConformanceTestBase<PartiQLStatement<*>, PartiQLResult>() {
    companion object {
        @JvmStatic
        @RegisterExtension
        val reporter = ReportGenerator("eval")
    }
    private val factory = EvalExecutor.Factory
    override val runner = TestRunner(factory)
}
