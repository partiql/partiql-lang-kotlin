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

import java.io.File
import kotlin.jvm.Throws

/**
 * Throws exceptions when the configured minimum branch coverage is not met by the current test suite.
 */
internal object ThresholdExecutor {

    @Throws(ThresholdException::class)
    internal fun execute(minimum: Double, reportPath: String, type: ThresholdException.ThresholdType) {
        val coverage = computeBranchCoverage(reportPath)
        if (coverage < minimum) {
            throw ThresholdException(minimum, coverage, type)
        }
    }

    private fun computeBranchCoverage(reportPath: String): Double {
        val file = File(reportPath)
        val lineReader = file.bufferedReader()
        var line = lineReader.readLine()
        var branchesFound = 0
        var branchesHit = 0
        while (line != null) {
            if (line.startsWith("BRF:")) {
                branchesFound += line.substring(4).toInt()
            }
            if (line.startsWith("BRH:")) {
                branchesHit += line.substring(4).toInt()
            }
            line = lineReader.readLine()
        }
        lineReader.close()
        return branchesHit.toDouble() / branchesFound
    }
}
