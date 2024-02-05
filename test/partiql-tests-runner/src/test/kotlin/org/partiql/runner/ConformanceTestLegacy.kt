package org.partiql.runner

import com.amazon.ion.IonValue
import org.junit.jupiter.api.extension.RegisterExtension
import org.partiql.lang.eval.ExprValue
import org.partiql.runner.executor.LegacyExecutor
import org.partiql.runner.report.ReportGenerator
import org.partiql.runner.test.TestRunner

/**
 * Runs the conformance tests without a fail list, so we can document the passing/failing tests in the conformance
 * report.
 *
 * These tests are excluded from normal testing/building unless the `conformanceReport` gradle property is
 * specified (i.e. `gradle test ... -PconformanceReport`)
 */
class ConformanceTestLegacy : ConformanceTestBase<ExprValue, IonValue>() {

    companion object {
        @JvmStatic
        @RegisterExtension
        val reporter = ReportGenerator("legacy")
    }

    private val factory = LegacyExecutor.Factory
    override val runner = TestRunner(factory)
}
