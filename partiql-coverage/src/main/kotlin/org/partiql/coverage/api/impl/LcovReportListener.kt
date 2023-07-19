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
    abstract fun getBranchCountKey(): String
    abstract fun getLineNumberOfBranchPrefix(): String
    abstract fun getResultOfBranchPrefix(): String

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
        val branchCount = map[getBranchCountKey()]?.toInt() ?: 0
        val branchToLineMap = mutableMapOf<String, Int>()
        val branchResults = mutableMapOf<String, Int>()

        var executedCount: Int = 0
        map.forEach { (key, value) ->
            when {
                key.startsWith(getLineNumberOfBranchPrefix()) -> {
                    val branchId = key.substring(getLineNumberOfBranchPrefix().length)
                    val lineNumber = value.toInt()
                    branchToLineMap[branchId] = lineNumber
                }
                key.startsWith(getResultOfBranchPrefix()) -> {
                    val branchId = key.substring(getResultOfBranchPrefix().length)
                    val lineNumber = value.toInt()
                    branchResults[branchId] = lineNumber
                    executedCount += value.toInt()
                }
            }
        }

        // Get ALL Branches Hit (including conditions)
        val lcovBranchesHit = branchResults.values.filter { it > 0 }.size
        val lcovBranchesFound = branchCount
        
        // Line Information
        // TODO: Fix this
        val lcovLinesFound = branchToLineMap.values.maxOrNull() ?: 0

        // Aggregate ALL Branch Information (including conditions)
        val lcovBranches = branchToLineMap.entries.map { (branchId, lineNumber) ->
            val count = branchResults[branchId] ?: 0
            Branch(branchId, count, lineNumber)
        }

        // TODO
        // Aggregate Line Data
        val count = 1
        val lcovLineData = branchToLineMap.values.toSet().map { lineNumber ->
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
