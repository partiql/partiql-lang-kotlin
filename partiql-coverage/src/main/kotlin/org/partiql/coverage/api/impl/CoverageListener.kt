package org.partiql.coverage.api.impl

import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.io.OutputStream

internal class CoverageListener : TestExecutionListener {

    private lateinit var reportStream: OutputStream
    private var destinationFileName = "build/partiql/coverage/report/cov.info"

    public object ReportKey {
        public const val DECISION_COUNT: String = "\$pql-dc"
        public const val PACKAGE_NAME: String = "\$pql-pan"
        public const val PROVIDER_NAME: String = "\$pql-prn"
        public const val ORIGINAL_STATEMENT: String = "\$pql-os"
        public const val LINE_NUMBER_OF_BRANCH_PREFIX: String = "\$pql-lfd_"
        public const val START_COLUMN_OF_BRANCH_PREFIX: String = "\$pql-scob_"
        public const val END_COLUMN_OF_BRANCH_PREFIX: String = "\$pql-ecob_"
        public const val RESULT_OF_BRANCH_PREFIX: String = "\$pql-rob_"
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        this.destinationFileName = testPlan?.configurationParameters?.get("destfile")?.let {
            when (it.isPresent) {
                true -> it.get()
                false -> destinationFileName
            }
        } ?: destinationFileName

        reportStream = initializeOutputFile(destinationFileName)
    }

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        val map = entry?.keyValuePairs ?: emptyMap()
        val decisionCount = map[ReportKey.DECISION_COUNT]?.toInt() ?: 0
        val originalStatement = map[ReportKey.ORIGINAL_STATEMENT] ?: ""
        val decisionToLineMap = mutableMapOf<Int, Int>()
        val decisionResults = mutableMapOf<Int, Int>()
        val packageName = map[ReportKey.PACKAGE_NAME]?.replace('.', '/') ?: "PQL_NO_PACKAGE_FOUND"
        val providerName = map[ReportKey.PROVIDER_NAME] ?: "PQL_NO_PROVIDER_FOUND_" + kotlin.random.Random(5).nextLong()
        var executedCount: Int = 0
        map.forEach { (key, value) ->
            when {
                key == ReportKey.DECISION_COUNT || key == ReportKey.ORIGINAL_STATEMENT -> {
                    // Do nothing for now
                }
                key.startsWith(ReportKey.LINE_NUMBER_OF_BRANCH_PREFIX) -> {
                    val branchId = key.substring(ReportKey.LINE_NUMBER_OF_BRANCH_PREFIX.length).toInt()
                    val lineNumber = value.toInt()
                    decisionToLineMap[branchId] = lineNumber
                }
                key.startsWith(ReportKey.RESULT_OF_BRANCH_PREFIX) -> {
                    val branchId = key.substring(ReportKey.RESULT_OF_BRANCH_PREFIX.length).toInt()
                    val lineNumber = value.toInt()
                    decisionResults[branchId] = lineNumber
                    executedCount += value.toInt()
                }
            }
        }

        // Aggregate Branch Information
        val branches = decisionToLineMap.entries.map { (decisionId, lineNumber) ->
            val count = decisionResults[decisionId] ?: 0
            Branch(decisionId.toString(), count, lineNumber)
        }

        // TODO
        // Aggregate Line Data
        val count = 1
        val lineData = decisionToLineMap.values.map { lineNumber ->
            LineData(lineNumber, count)
        }

        // Write Query to File
        val uniqueFileName = "$providerName.pql"
        val queryPath = File("build/partiql/coverage/source/$packageName/$uniqueFileName")
        writePartiQLToFile(originalStatement, queryPath)

        // Write to Coverage Report File
        val coverageEntry = getCoverageInformationEntry(
            filePath = queryPath.absolutePath,
            branchesFound = decisionCount,
            branchesHit = decisionCount, // TODO: Fix this
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

    private fun initializeOutputFile(destFile: String): OutputStream { 
        val file = File(destFile)
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
        strBuilder.appendLine("TN:")

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
