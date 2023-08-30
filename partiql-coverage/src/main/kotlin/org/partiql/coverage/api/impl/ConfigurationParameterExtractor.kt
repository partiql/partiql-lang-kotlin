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

import org.junit.platform.launcher.TestPlan

internal object ConfigurationParameterExtractor {

    internal class ConfigParams(
        val lcovBranchConfig: LcovConfig?,
        val lcovConditionConfig: LcovConfig?
    )

    internal class LcovConfig(
        val reportPath: String,
        val htmlOutputDir: String?,
        val minimum: Double?
    )

    internal fun extract(testPlan: TestPlan): ConfigParams {
        val config = ConfigParamWrapper(testPlan.configurationParameters)

        val lcovBranchConfig = config.getConfigParam(ConfigurationParameter.LCOV_BRANCH_ENABLED)?.let {
            when (it.toBoolean()) {
                false -> null
                true -> {
                    val reportPath = config.getConfigParam(ConfigurationParameter.LCOV_BRANCH_REPORT_LOCATION)
                        ?: error("Expected to find a report path (specified by \"${ConfigurationParameter.LCOV_BRANCH_REPORT_LOCATION.key}\").")
                    val htmlOutputDir = config.getConfigParam(ConfigurationParameter.LCOV_BRANCH_HTML_DIR)
                    val branchMinimum = config.getConfigParam(ConfigurationParameter.LCOV_BRANCH_MINIMUM)?.toDouble()
                    LcovConfig(reportPath, htmlOutputDir, branchMinimum)
                }
            }
        }

        val lcovConditionConfig = config.getConfigParam(ConfigurationParameter.LCOV_BRANCH_CONDITION_ENABLED)?.let {
            when (it.toBoolean()) {
                false -> null
                true -> {
                    val reportPath = config.getConfigParam(ConfigurationParameter.LCOV_BRANCH_CONDITION_REPORT_LOCATION)
                        ?: error("Expected to find a report path (specified by \"${ConfigurationParameter.LCOV_BRANCH_CONDITION_REPORT_LOCATION.key}\").")
                    val htmlOutputDir = config.getConfigParam(ConfigurationParameter.LCOV_BRANCH_CONDITION_HTML_DIR)
                    val branchMinimum = config.getConfigParam(ConfigurationParameter.LCOV_BRANCH_CONDITION_MINIMUM)?.toDouble()
                    LcovConfig(reportPath, htmlOutputDir, branchMinimum)
                }
            }
        }

        return ConfigParams(lcovBranchConfig, lcovConditionConfig)
    }

    private class ConfigParamWrapper(val params: org.junit.platform.engine.ConfigurationParameters) {
        fun getConfigParam(param: ConfigurationParameter): String? {
            val value = this.params[param.key]
            return when (value.isPresent) {
                true -> value.get()
                false -> null
            }
        }
    }
}
