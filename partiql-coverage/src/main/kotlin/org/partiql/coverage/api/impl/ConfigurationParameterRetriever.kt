package org.partiql.coverage.api.impl

import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

/**
 * When registered, this class exposes JUnit's Configuration Parameters that are relevant to PartiQL Code Coverage. This
 * class specifically waits until the test plan has been started to load the parameters.
 */
internal class ConfigurationParameterRetriever : TestExecutionListener {
    var plan: TestPlan? = null

    /**
     * Users should NOT call this until the test plan has begun execution.
     */
    internal fun getConfig(): ConfigurationParameterExtractor.ConfigParams? = when (plan) {
        null -> null
        else -> ConfigurationParameterExtractor.extract(plan!!)
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        if (testPlan == null) { return super.testPlanExecutionStarted(testPlan) }
        plan = testPlan
        super.testPlanExecutionStarted(testPlan)
    }
}
