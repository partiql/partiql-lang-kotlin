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
