package org.partiql.coverage.api.impl

import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.io.OutputStream
import java.util.Random

internal class CoverageListener : TestExecutionListener {

    private lateinit var reportStream: OutputStream
    private lateinit var destinationFileName: String
    private lateinit var reportFile: File
    private var isLcovEnabled: Boolean = false

    public object ReportKey {
        public const val DECISION_COUNT: String = "\$pql-dc"
        public const val PACKAGE_NAME: String = "\$pql-pan"
        public const val PROVIDER_NAME: String = "\$pql-prn"
        public const val ORIGINAL_STATEMENT: String = "\$pql-os"
        public const val LINE_NUMBER_OF_BRANCH_PREFIX: String = "\$pql-lfd_"
        public const val RESULT_OF_BRANCH_PREFIX: String = "\$pql-rob_"
    }

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
        val decisionCount = map[ReportKey.DECISION_COUNT]?.toInt() ?: 0
        val originalStatement = map[ReportKey.ORIGINAL_STATEMENT] ?: ""
        val decisionToLineMap = mutableMapOf<String, Int>()
        val decisionResults = mutableMapOf<String, Int>()
        val packageName = map[ReportKey.PACKAGE_NAME]?.replace('.', '/') ?: "PQL_NO_PACKAGE_FOUND"
        val providerName = map[ReportKey.PROVIDER_NAME] ?: "PQL_NO_PROVIDER_FOUND_" + kotlin.random.Random(5).nextLong()
        var executedCount: Int = 0
        map.forEach { (key, value) ->
            when {
                key == ReportKey.DECISION_COUNT || key == ReportKey.ORIGINAL_STATEMENT -> {
                    // Do nothing for now
                }
                key.startsWith(ReportKey.LINE_NUMBER_OF_BRANCH_PREFIX) -> {
                    val branchId = key.substring(ReportKey.LINE_NUMBER_OF_BRANCH_PREFIX.length)
                    val lineNumber = value.toInt()
                    decisionToLineMap[branchId] = lineNumber
                }
                key.startsWith(ReportKey.RESULT_OF_BRANCH_PREFIX) -> {
                    val branchId = key.substring(ReportKey.RESULT_OF_BRANCH_PREFIX.length)
                    val lineNumber = value.toInt()
                    decisionResults[branchId] = lineNumber
                    executedCount += value.toInt()
                }
            }
        }

        // Get Branches Hit
        val branchesHit = decisionResults.values.filter { it > 0 }.size

        // Aggregate Branch Information
        val branches = decisionToLineMap.entries.map { (decisionId, lineNumber) ->
            val count = decisionResults[decisionId] ?: 0
            Branch(decisionId, count, lineNumber)
        }

        // TODO
        // Aggregate Line Data
        val count = 1
        val lineData = decisionToLineMap.values.toSet().map { lineNumber ->
            LineData(lineNumber, count)
        }

        // Write Query to File
        val uniqueFileName = "$providerName.pql"
        val queryPath = reportFile.parentFile.resolve("source").resolve(packageName).resolve(uniqueFileName)
        writePartiQLToFile(originalStatement, queryPath)

        // Write to Coverage Report File
        val coverageEntry = getCoverageInformationEntry(
            filePath = queryPath.absolutePath,
            branchesFound = decisionCount,
            branchesHit = branchesHit,
            linesFound = decisionToLineMap.values.maxOrNull()!!,
            linesHit = decisionToLineMap.values.maxOrNull()!!, // TODO: Fix this
            branches = branches,
            lineData = lineData
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
        val randomTestName = Random(5).nextInt().toString() // TODO: Update this
        strBuilder.appendLine("TN:$randomTestName")

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
