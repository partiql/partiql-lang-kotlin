package org.partiql.coverage.api.impl

import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.io.OutputStream

internal class LcovReportListener : TestExecutionListener {

    private lateinit var reportStream: OutputStream
    private lateinit var destinationFileName: String
    private lateinit var reportFile: File
    private var isLcovEnabled: Boolean = false

    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        if (testPlan == null) { return super.testPlanExecutionStarted(testPlan) }

        // Get Configuration Parameters
        val configParams = ConfigurationParameterExtractor.extract(testPlan)
        isLcovEnabled = configParams.lcovConfig != null
        when (isLcovEnabled) {
            true -> {
                destinationFileName = configParams.lcovConfig?.reportPath
                    ?: throw Exception("LCOV Report location not set. Please set system property (${ConfigurationParameters.LCOV_REPORT_LOCATION.key})")
                reportFile = File(destinationFileName)
                reportStream = initializeOutputFile(reportFile)
            }
            false -> {
                reportStream = OutputStream.nullOutputStream()
            }
        }
    }

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        if (!isLcovEnabled) { return super.reportingEntryPublished(testIdentifier, entry) }

        val map = entry?.keyValuePairs ?: emptyMap()
        val originalStatement = map[ReportKey.ORIGINAL_STATEMENT] ?: ""
        val packageName = map[ReportKey.PACKAGE_NAME]?.replace('.', '/') ?: "PQL_NO_PACKAGE_FOUND"
        val providerName = map[ReportKey.PROVIDER_NAME] ?: "PQL_NO_PROVIDER_FOUND_" + kotlin.random.Random(5).nextLong()

        // Condition Information (Boolean Expressions)
        val conditionCount = map[ReportKey.CONDITION_COUNT]?.toInt() ?: 0
        val conditionToLineMap = mutableMapOf<String, Int>()
        val conditionResults = mutableMapOf<String, Int>()

        // Branch Information (CASE, WHERE, HAVING)
        val branchCount = map[ReportKey.BRANCH_COUNT]?.toInt() ?: 0
        val branchToLineMap = mutableMapOf<String, Int>()
        val branchResults = mutableMapOf<String, Int>()

        var executedCount: Int = 0
        map.forEach { (key, value) ->
            when {
                key == ReportKey.CONDITION_COUNT || key == ReportKey.ORIGINAL_STATEMENT -> {
                    // Do nothing for now
                }
                key.startsWith(ReportKey.LINE_NUMBER_OF_BRANCH_PREFIX) -> {
                    val branchId = key.substring(ReportKey.LINE_NUMBER_OF_BRANCH_PREFIX.length)
                    val lineNumber = value.toInt()
                    branchToLineMap[branchId] = lineNumber
                }
                key.startsWith(ReportKey.LINE_NUMBER_OF_CONDITION_PREFIX) -> {
                    val conditionId = key.substring(ReportKey.LINE_NUMBER_OF_CONDITION_PREFIX.length)
                    val lineNumber = value.toInt()
                    conditionToLineMap[conditionId] = lineNumber
                }
                key.startsWith(ReportKey.RESULT_OF_CONDITION_PREFIX) -> {
                    val conditionId = key.substring(ReportKey.RESULT_OF_CONDITION_PREFIX.length)
                    val lineNumber = value.toInt()
                    conditionResults[conditionId] = lineNumber
                    executedCount += value.toInt()
                }
                key.startsWith(ReportKey.RESULT_OF_BRANCH_PREFIX) -> {
                    val branchId = key.substring(ReportKey.RESULT_OF_BRANCH_PREFIX.length)
                    val lineNumber = value.toInt()
                    branchResults[branchId] = lineNumber
                    executedCount += value.toInt()
                }
            }
        }

        // Get ALL Branches Hit (including conditions)
        val lcovBranchesHit = conditionResults.values.filter { it > 0 }.size + branchResults.values.filter { it > 0 }.size
        val lcovBranchesFound = conditionCount + branchCount
        
        // Line Information
        // TODO: Fix this
        val lcovLinesFound = (conditionToLineMap.values + branchToLineMap.values).maxOrNull()!!

        // Aggregate ALL Branch Information (including conditions)
        val lcovBranches = conditionToLineMap.entries.map { (conditionId, lineNumber) ->
            val count = conditionResults[conditionId] ?: 0
            Branch(conditionId, count, lineNumber)
        } + branchToLineMap.entries.map { (branchId, lineNumber) ->
            val count = branchResults[branchId] ?: 0
            Branch(branchId, count, lineNumber)
        }

        // TODO
        // Aggregate Line Data
        val count = 1
        val lcovLineData = (conditionToLineMap.values + branchToLineMap.values).toSet().map { lineNumber ->
            LineData(lineNumber, count)
        }

        // Write Query to File
        val uniqueFileName = "$providerName.pql"
        val queryPath = reportFile.parentFile.resolve("source").resolve(packageName).resolve(uniqueFileName)
        writePartiQLToFile(originalStatement, queryPath)

        // Write to Coverage Report File
        val coverageEntry = getCoverageInformationEntry(
            testName = testIdentifier?.uniqueId ?: "NO_TEST_NAME",
            filePath = queryPath.absolutePath,
            branchesFound = lcovBranchesFound,
            branchesHit = lcovBranchesHit,
            linesFound = lcovLinesFound,
            linesHit = lcovLinesFound,
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
        branches: List<Branch>,
        lineData: List<LineData>
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
        val count: Int,
        val line: Int
    )

    private data class LineData(
        val line: Int,
        val executionCount: Int
    )
}
