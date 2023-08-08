package org.partiql.coverage.api.impl

import java.io.File

/**
 * Throws exceptions when the configured minimum branch coverage is not met by the current test suite.
 */
internal object ThresholdExecutor {

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
