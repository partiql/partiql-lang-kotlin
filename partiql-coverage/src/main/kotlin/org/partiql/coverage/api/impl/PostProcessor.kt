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

        // Generate HTML Report
        val lcovBranchConfig = configParams.lcovBranchConfig
        if (lcovBranchConfig != null) {
            if (lcovBranchConfig.htmlOutputDir != null) {
                HtmlWriter.write(
                    reportPath = lcovBranchConfig.reportPath,
                    htmlOutputDir = lcovBranchConfig.htmlOutputDir,
                    title = "PartiQL Code Coverage (Branch) Report"
                )
            }

            // Assert Branch Coverage Threshold
            if (lcovBranchConfig.minimum != null) {
                ThresholdExecutor.execute(
                    minimum = lcovBranchConfig.minimum,
                    reportPath = lcovBranchConfig.reportPath,
                    type = ThresholdException.ThresholdType.BRANCH
                )
            }
        }

        val lcovConditionConfig = configParams.lcovConditionConfig
        if (lcovConditionConfig != null) {
            if (lcovConditionConfig.htmlOutputDir != null) {
                HtmlWriter.write(
                    reportPath = lcovConditionConfig.reportPath,
                    htmlOutputDir = lcovConditionConfig.htmlOutputDir,
                    title = "PartiQL Code Coverage (Condition) Report"
                )
            }

            // Assert Branch Coverage Threshold
            if (lcovConditionConfig.minimum != null) {
                ThresholdExecutor.execute(
                    minimum = lcovConditionConfig.minimum,
                    reportPath = lcovConditionConfig.reportPath,
                    type = ThresholdException.ThresholdType.CONDITION
                )
            }
        }

        super.launcherSessionClosed(session)
    }


}