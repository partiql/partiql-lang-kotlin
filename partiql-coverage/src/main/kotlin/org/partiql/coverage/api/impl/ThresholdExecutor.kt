package org.partiql.coverage.api.impl

import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.math.BigDecimal

internal class ThresholdExecutor : LauncherSessionListener {
    internal var branchMin: BigDecimal? = null
    internal var customReportPath: String? = null

    override fun launcherSessionOpened(session: LauncherSession?) {
        session?.launcher?.registerTestExecutionListeners(ConfigurationParameterRetriever()) ?: run {
            println("LAUNCHER SESSION DID NOT EXIST")
        }
        super.launcherSessionOpened(session)
    }

    override fun launcherSessionClosed(session: LauncherSession?) {
        println("BRANCH MINIMUM     : $branchMin")
        println("CUSTOM REPORT PATH : $customReportPath")
        if (branchMin == null) { return super.launcherSessionClosed(session) }
        val reportPath = when (customReportPath) {
            null -> ConfigurationParameters.REPORT_LOCATION_DEFAULT_VALUE
            else -> customReportPath!!
        }
        println("REPORT PATH       : $customReportPath")

        val branchCoverage = loadLcovInfoFile(reportPath)
        println("FOUND BRANCH COVERAGE: $branchCoverage")
        if (branchCoverage.toBigDecimal() < branchMin) {
            println("FAILED!!!!")
        }
        println("LAUNCHER SESSION CLOSED WITH BRANCH MINIMUM SET TO BE $branchMin")
        super.launcherSessionClosed(session)
    }

    private fun loadLcovInfoFile(reportPath: String): Double {
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
        val branchCoverage = branchesHit.toDouble() / branchesFound
        println("BRANCHES FOUND : $branchesFound")
        println("BRANCHES HIT   : $branchesHit")
        println("COVERAGE       : $branchCoverage")
        return branchCoverage
    }

    private inner class ConfigurationParameterRetriever : TestExecutionListener {
        override fun testPlanExecutionStarted(testPlan: TestPlan?) {
            if (testPlan == null) { return super.testPlanExecutionStarted(testPlan) }

            // Gets the Report's Path
            val reportLocation = testPlan.configurationParameters[ConfigurationParameters.REPORT_LOCATION_KEY]
            if (reportLocation.isPresent) { customReportPath = reportLocation.get() }

            // Gets the Branch Coverage Minimum
            val branchMinimum = testPlan.configurationParameters[ConfigurationParameters.BRANCH_MINIMUM_KEY]
            if (branchMinimum.isPresent) { branchMin = branchMinimum.get().toBigDecimal() }

            super.testPlanExecutionStarted(testPlan)
        }
    }
}