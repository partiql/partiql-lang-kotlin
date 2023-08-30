/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
