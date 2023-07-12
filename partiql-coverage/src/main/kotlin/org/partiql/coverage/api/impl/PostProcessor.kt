package org.partiql.coverage.api.impl

import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

/**
 * This class is in charge of running two things:
 * 1. The Threshold
 */
internal class PostProcessor : LauncherSessionListener {
    internal var customBranchMin: Double? = null
    internal var customReportPath: String? = null
    internal var customHtmlOutputDirectory: String? = null

    /**
     * Registers the [ConfigurationParameterRetriever].
     */
    override fun launcherSessionOpened(session: LauncherSession?) {
        session?.launcher?.registerTestExecutionListeners(ConfigurationParameterRetriever())
        super.launcherSessionOpened(session)
    }

    /**
     * Conditionally triggers the [ThresholdExecutor] and the [HtmlWriter].
     */
    override fun launcherSessionClosed(session: LauncherSession?) {
        // Set Values or Grab Defaults
        val reportPath = customReportPath ?: ConfigurationParameters.REPORT_LOCATION.default as String
        val htmlOutputDir = customHtmlOutputDirectory ?: ConfigurationParameters.OUTPUT_HTML_DIR.default as String
        val branchMin = customBranchMin ?: ConfigurationParameters.BRANCH_MINIMUM.default as Double?

        // Generate HTML Report
        HtmlWriter.write(reportPath, htmlOutputDir)

        // Assert Branch Coverage Threshold
        if (branchMin != null) {
            ThresholdExecutor.execute(customBranchMin!!, reportPath)
        }

        super.launcherSessionClosed(session)
    }

    private inner class ConfigurationParameterRetriever : TestExecutionListener {
        override fun testPlanExecutionStarted(testPlan: TestPlan?) {
            if (testPlan == null) { return super.testPlanExecutionStarted(testPlan) }

            // Gets the Report's Path
            val reportLocation = testPlan.configurationParameters[ConfigurationParameters.REPORT_LOCATION.key]
            if (reportLocation.isPresent) { customReportPath = reportLocation.get() }

            // Gets the Branch Coverage Minimum
            val branchMinimum = testPlan.configurationParameters[ConfigurationParameters.BRANCH_MINIMUM.key]
            if (branchMinimum.isPresent) { customBranchMin = branchMinimum.get().toDouble() }

            // Gets the Branch Coverage Minimum
            val outputHtmlDir = testPlan.configurationParameters[ConfigurationParameters.OUTPUT_HTML_DIR.key]
            if (outputHtmlDir.isPresent) { customHtmlOutputDirectory = outputHtmlDir.get() }

            super.testPlanExecutionStarted(testPlan)
        }
    }
}