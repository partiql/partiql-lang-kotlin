package org.partiql.coverage.api.impl

import org.partiql.coverage.api.MinimumThresholdException
import java.io.File

/**
 * Throws exceptions when the configured minimum branch coverage is not met by the current test suite.
 */
internal object ThresholdExecutor {

    internal fun execute(branchMin: Double?, conditionMin: Double?, reportPath: String) {
        val coverage = computeBranchCoverage(reportPath)

        if (branchMin != null) {
            if (coverage.branch < branchMin) {
                throw MinimumThresholdException(branchMin, coverage.branch, MinimumThresholdException.ThresholdType.BRANCH)
            }
        }

        if (conditionMin != null) {
            if (coverage.condition < conditionMin) {
                throw MinimumThresholdException(conditionMin, coverage.condition, MinimumThresholdException.ThresholdType.CONDITION)
            }
        }
    }

    private fun computeBranchCoverage(reportPath: String): Coverage {
        val file = File(reportPath)
        val lineReader = file.bufferedReader()
        var line = lineReader.readLine()
        var branchesFound = 0
        var branchesHit = 0
        var conditionsFound = 0
        var conditionsHit = 0
        while (line != null) {
            if (line.startsWith("BRDA:")) {
                val x = line.split(',')
                val name = x[2]
                val wasHitCounter = x[3].let {
                    when {
                        it == "-" -> 0
                        it.toInt() == 0 -> 0
                        else -> 1
                    }
                }
                when (name[0]) {
                    'B' -> {
                        branchesFound += 1
                        branchesHit += wasHitCounter
                    }
                    'C' -> {
                        conditionsFound += 1
                        conditionsHit += wasHitCounter
                    }
                    else -> error("Received malformed LCOV report.")
                }
            }
            line = lineReader.readLine()
        }
        lineReader.close()
        val branchCoverage = branchesHit.toDouble() / branchesFound
        val conditionCoverage = conditionsHit.toDouble() / conditionsFound
        return Coverage(
            branchCoverage,
            conditionCoverage
        )
    }

    private class Coverage(
        val branch: Double,
        val condition: Double
    )
}
