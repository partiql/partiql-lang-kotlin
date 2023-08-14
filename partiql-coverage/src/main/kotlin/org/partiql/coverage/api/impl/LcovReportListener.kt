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

import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.io.OutputStream

internal abstract class LcovReportListener : TestExecutionListener {

    private lateinit var reportStream: OutputStream
    private lateinit var destinationFileName: String
    private lateinit var reportFile: File

    abstract fun isLcovEnabled(): Boolean
    abstract fun getReportPath(): String
    abstract fun getTargetCountKey(): String
    abstract fun getCoverageTargetType(): ReportKey.CoverageTarget

    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        if (testPlan == null) { return super.testPlanExecutionStarted(testPlan) }
        when (isLcovEnabled()) {
            true -> {
                destinationFileName = getReportPath()
                reportFile = File(destinationFileName)
                reportStream = initializeOutputFile(reportFile)
            }
            false -> {
                reportStream = OutputStream.nullOutputStream()
            }
        }
    }

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        if (!isLcovEnabled()) { return super.reportingEntryPublished(testIdentifier, entry) }

        val map = entry?.keyValuePairs ?: emptyMap()
        val originalStatement = map[ReportKey.ORIGINAL_STATEMENT] ?: ""
        val packageName = map[ReportKey.PACKAGE_NAME]?.replace('.', '/') ?: "PQL_NO_PACKAGE_FOUND"
        val providerName = map[ReportKey.PROVIDER_NAME] ?: "PQL_NO_PROVIDER_FOUND_" + kotlin.random.Random(5).nextLong()

        // Branch Information (CASE, WHERE, HAVING)
        val branchCount = map[getTargetCountKey()]?.toInt() ?: 0
        val branchToLineMap = mutableMapOf<String, Long>()
        val branchResults = mutableMapOf<String, Long>()

        // Represents Branch/Condition Names
        val targetIds = map.filter { it.key.startsWith(ReportKey.COVERAGE_TARGET_PREFIX) }.filter {
            ReportKey.CoverageTarget.valueOf(it.value) == getCoverageTargetType()
        }.keys.map { it.split(ReportKey.DELIMITER)[1] }.toSet()

        val lcovBranches = mutableListOf<Branch>()
        val lcovLineData = mutableSetOf<LineData>()
        var executedCount = 0L
        targetIds.forEach { targetId ->
            val lineNum = map[ReportKey.LINE_NUMBER_OF_TARGET_PREFIX + ReportKey.DELIMITER + targetId]?.toLong() ?: 1L
            val outcome = map[ReportKey.OUTCOME_OF_TARGET_PREFIX + ReportKey.DELIMITER + targetId] ?: "UNKNOWN_OUTCOME"
            val type = map[ReportKey.TYPE_OF_TARGET_PREFIX + ReportKey.DELIMITER + targetId] ?: "UNKNOWN_TYPE"
            val targetName = "$type${ReportKey.DELIMITER}$outcome${ReportKey.DELIMITER}$targetId"
            val count = map[ReportKey.TARGET_COUNT_PREFIX + ReportKey.DELIMITER + targetId]?.toLong() ?: 0L
            executedCount += count
            branchResults[targetName] = count
            branchToLineMap[targetName] = lineNum
            lcovBranches.add(Branch(targetName, count, lineNum))
            lcovLineData.add(LineData(lineNum, 1)) // NOTE: This is inaccurate.
        }

        // Write Query to File
        val uniqueFileName = "$providerName.pql"
        val queryPath = reportFile.parentFile.resolve("source").resolve(packageName).resolve(uniqueFileName)
        writePartiQLToFile(originalStatement, queryPath)

        // Write to Coverage Report File
        val coverageEntry = getCoverageInformationEntry(
            testName = testIdentifier?.uniqueId ?: "NO_TEST_NAME",
            filePath = queryPath.absolutePath,
            branchesFound = branchCount,
            branchesHit = lcovBranches.filter { it.count > 0 }.size,
            linesFound = lcovLineData.size, // NOTE: This is inaccurate.
            linesHit = lcovLineData.size, // NOTE: This is inaccurate.
            branches = lcovBranches,
            lineData = lcovLineData
        )
        reportStream.write(coverageEntry.toByteArray())
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        reportStream.flush()
        reportStream.close()
    }

    //
    //
    // PRIVATE HELPER FUNCTIONS
    //
    //

    private fun initializeOutputFile(file: File): OutputStream {
        file.parentFile.mkdirs()
        return file.outputStream()
    }

    private fun writePartiQLToFile(stmt: String, file: File) {
        file.parentFile.mkdirs()
        file.writeBytes(stmt.toByteArray())
    }

    private fun getCoverageInformationEntry(
        testName: String,
        filePath: String,
        branchesFound: Int,
        branchesHit: Int,
        linesFound: Int,
        linesHit: Int,
        branches: Iterable<Branch>,
        lineData: Iterable<LineData>
    ): String {
        val strBuilder = StringBuilder()

        // Test Name
        strBuilder.appendLine("TN:$testName")

        // Source File Path
        strBuilder.appendLine("SF:$filePath")

        // Branch Coverage Data
        val block = 0
        branches.forEach { branch ->
            strBuilder.appendLine("BRDA:${branch.line},$block,${branch.name},${branch.count}")
        }

        // Branch Summary Information
        strBuilder.appendLine("BRF:$branchesFound")
        strBuilder.appendLine("BRH:$branchesHit")

        // Line Coverage Data
        lineData.forEach { line ->
            strBuilder.appendLine("DA:${line.line},${line.executionCount}")
        }

        // Line Coverage Summary
        strBuilder.appendLine("LH:$linesHit")
        strBuilder.appendLine("LF:$linesFound")

        // End
        strBuilder.appendLine("end_of_record")
        return strBuilder.toString()
    }

    private data class Branch(
        val name: String,
        val count: Long,
        val line: Long
    )

    private data class LineData(
        val line: Long,
        val executionCount: Long
    )

    internal class LcovReportConditionListener : LcovReportListener() {
        private var isLcovEnabled: Boolean = false
        private var reportPath: String? = null

        override fun isLcovEnabled(): Boolean = this.isLcovEnabled

        override fun getReportPath(): String = this.reportPath!!

        override fun getTargetCountKey(): String = ReportKey.BRANCH_CONDITION_COUNT

        override fun getCoverageTargetType(): ReportKey.CoverageTarget = ReportKey.CoverageTarget.BRANCH_CONDITION

        override fun testPlanExecutionStarted(testPlan: TestPlan?) {
            if (testPlan == null) { return super.testPlanExecutionStarted(testPlan) }

            // Get Configuration Parameters
            val configParams = ConfigurationParameterExtractor.extract(testPlan)
            isLcovEnabled = configParams.lcovConditionConfig != null
            this.reportPath = configParams.lcovConditionConfig?.reportPath
            return super.testPlanExecutionStarted(testPlan)
        }
    }

    internal class LcovReportBranchListener : LcovReportListener() {
        private var isLcovEnabled: Boolean = false
        private var reportPath: String? = null

        override fun isLcovEnabled(): Boolean = this.isLcovEnabled

        override fun getReportPath(): String = this.reportPath!!

        override fun getTargetCountKey(): String = ReportKey.BRANCH_COUNT

        override fun getCoverageTargetType(): ReportKey.CoverageTarget = ReportKey.CoverageTarget.BRANCH

        override fun testPlanExecutionStarted(testPlan: TestPlan?) {
            if (testPlan == null) { return super.testPlanExecutionStarted(testPlan) }

            // Get Configuration Parameters
            val configParams = ConfigurationParameterExtractor.extract(testPlan)
            isLcovEnabled = configParams.lcovBranchConfig != null
            this.reportPath = configParams.lcovBranchConfig?.reportPath
            return super.testPlanExecutionStarted(testPlan)
        }
    }
}
