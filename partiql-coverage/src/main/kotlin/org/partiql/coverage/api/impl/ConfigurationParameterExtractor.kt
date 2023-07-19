package org.partiql.coverage.api.impl

import org.junit.platform.launcher.TestPlan

internal object ConfigurationParameterExtractor {

    internal class ConfigParams(
        val lcovConfig: LcovConfig?,
        val htmlConfig: HtmlConfig?,
        val thresholdConfig: ThresholdConfig?
    ) {
        init {
            if (lcovConfig == null) {
                if (htmlConfig != null) {
                    throw Exception("Cannot set PartiQL Code Coverage HTML Configurations unless LCOV is enabled.")
                }
                if (thresholdConfig != null) {
                    throw Exception("Cannot set PartiQL Code Coverage Threshold Configurations unless LCOV is enabled.")
                }
            }
        }
    }

    internal class LcovConfig(val reportPath: String)

    internal class HtmlConfig(val outputDir: String)

    internal class ThresholdConfig(val branchMinimum: Double?, val conditionMinimum: Double?)

    internal fun extract(testPlan: TestPlan): ConfigParams {
        var isLcovEnabled: Boolean = false
        var reportPath: String? = null
        var customBranchMin: Double? = null
        var customConditionMin: Double? = null
        var isHtmlEnabled: Boolean = false
        var customHtmlOutputDirectory: String? = null

        // Gets LCOV Enabled
        val lcovEnabled = testPlan.configurationParameters[ConfigurationParameters.LCOV_ENABLED.key]
        if (lcovEnabled.isPresent) { isLcovEnabled = lcovEnabled.get().toBoolean() }

        // Gets the LCOV Report's Path
        val reportLocation = testPlan.configurationParameters[ConfigurationParameters.LCOV_REPORT_LOCATION.key]
        if (reportLocation.isPresent) { reportPath = reportLocation.get() }

        // Gets the Branch Coverage Minimum
        val branchMinimum = testPlan.configurationParameters[ConfigurationParameters.BRANCH_MINIMUM.key]
        if (branchMinimum.isPresent) { customBranchMin = branchMinimum.get().toDouble() }

        // Gets the Branch Coverage Minimum
        val conditionMinimum = testPlan.configurationParameters[ConfigurationParameters.CONDITION_MINIMUM.key]
        if (conditionMinimum.isPresent) { customConditionMin = conditionMinimum.get().toDouble() }

        // Gets whether HTML is enabled
        val htmlEnabled = testPlan.configurationParameters[ConfigurationParameters.LCOV_HTML_ENABLED.key]
        if (htmlEnabled.isPresent) { isHtmlEnabled = htmlEnabled.get().toBoolean() }

        // Gets the HTML Output Dir
        val outputHtmlDir = testPlan.configurationParameters[ConfigurationParameters.LCOV_HTML_OUTPUT_DIR.key]
        if (outputHtmlDir.isPresent) { customHtmlOutputDirectory = outputHtmlDir.get() }

        // Create Configuration Object
        val lcov = when (isLcovEnabled) {
            true -> LcovConfig(
                reportPath = reportPath
                    ?: throw Exception("Expected an output PartiQL Code Coverage report path.")
            )
            false -> null
        }
        val html = when (isHtmlEnabled) {
            true -> HtmlConfig(
                outputDir = customHtmlOutputDirectory ?: throw Exception("Expected an output PartiQL Code Coverage HTML directory.")
            )
            false -> null
        }
        val thresholdConfig = when (customBranchMin) {
            null -> null
            else -> ThresholdConfig(customBranchMin, customConditionMin)
        }
        return ConfigParams(lcov, html, thresholdConfig)
    }
}