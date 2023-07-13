package org.partiql.coverage.api.impl

import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * This class is in charge of:
 * 1. Retrieving the JUnit5 Configuration Parameters relevant to PartiQL Code Coverage
 * 2. Conditionally executing the [ThresholdExecutor].
 * 3. Conditionally executing the [HtmlWriter].
 */
internal class PostProcessor : LauncherSessionListener {
    private val configurationParameterRetriever = ConfigurationParameterRetriever()

    /**
     * Registers the [ConfigurationParameterRetriever].
     */
    override fun launcherSessionOpened(session: LauncherSession?) {
        session?.launcher?.registerTestExecutionListeners(configurationParameterRetriever)
        super.launcherSessionOpened(session)
    }

    /**
     * Conditionally triggers the [ThresholdExecutor] and the [HtmlWriter].
     */
    override fun launcherSessionClosed(session: LauncherSession?) {
        // Get Configuration
        val configParams = configurationParameterRetriever.getConfig()
            ?: error("Configuration Parameters should have been initialized.")

        // Ensure PartiQL Code Coverage is Enabled
        if (configParams.lcovConfig == null) { return super.launcherSessionClosed(session) }
        val reportPath = configParams.lcovConfig.reportPath

        // Generate HTML Report
        if (configParams.htmlConfig != null) {
            HtmlWriter.write(
                reportPath = reportPath,
                htmlOutputDir = configParams.htmlConfig.outputDir
            )
        }

        // Assert Branch Coverage Threshold
        if (configParams.thresholdConfig != null) {
            ThresholdExecutor.execute(
                branchMin = configParams.thresholdConfig.branchMinimum,
                reportPath = reportPath
            )
        }

        super.launcherSessionClosed(session)
    }


}