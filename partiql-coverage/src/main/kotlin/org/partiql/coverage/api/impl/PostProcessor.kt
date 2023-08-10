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

        // Get Applicable Configurations
        val configInfos = listOfNotNull(
            configParams.lcovBranchConfig?.let {
                ConfigInfo("PartiQL Code Coverage (Branch) Report", it, ThresholdException.ThresholdType.BRANCH)
            },
            configParams.lcovConditionConfig?.let {
                ConfigInfo("PartiQL Code Coverage (Branch-Condition) Report", it, ThresholdException.ThresholdType.CONDITION)
            }
        )

        // Generate HTML Report(s)
        configInfos.map { configInfo ->
            configInfo.config.htmlOutputDir?.let { htmlDir ->
                HtmlWriter.write(
                    reportPath = configInfo.config.reportPath,
                    htmlOutputDir = htmlDir,
                    title = configInfo.title
                )
            }
        }

        // Check Threshold(s)
        configInfos.map { configInfo ->
            configInfo.config.minimum?.let { minimum ->
                ThresholdExecutor.execute(
                    minimum = minimum,
                    reportPath = configInfo.config.reportPath,
                    type = configInfo.type
                )
            }
        }
        super.launcherSessionClosed(session)
    }

    private class ConfigInfo(
        val title: String,
        val config: ConfigurationParameterExtractor.LcovConfig,
        val type: ThresholdException.ThresholdType
    )
}
