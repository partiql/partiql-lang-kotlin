package org.partiql.coverage.api.impl

import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.OutputStream

internal class CoverageListener : TestExecutionListener {

    private lateinit var stream: OutputStream
    private var destinationFileName = "cov.xml"
    private var destinationDir = "build/partiql/coverage"

    public object ReportKey {
        public const val DECISION_COUNT: String = "\$pql-dc"
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

        this.destinationDir = testPlan?.configurationParameters?.get("destdir")?.let {
            when (it.isPresent) {
                true -> it.get()
                false -> destinationDir
            }
        } ?: destinationDir

        // TODO: Write to destination path
        stream = System.out
    }

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        val map = entry?.keyValuePairs ?: emptyMap()
        val decisionCount = map[ReportKey.DECISION_COUNT]?.toInt() ?: 0
        val originalStatement = map[ReportKey.ORIGINAL_STATEMENT] ?: ""
        val decisionToLineMap = mutableMapOf<Int, Int>()
        val decisionResults = mutableMapOf<Int, Int>()
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
        val branchCoverage = executedCount / decisionCount.toDouble() * 100

        // TODO: Write to file
        // stream.write("Entry reported! $map\n".toByteArray())
        stream.write("------------------------\n".toByteArray())
        stream.write("TOTAL POSSIBLE DECISION COUNT: $decisionCount\n".toByteArray())
        stream.write("EXECUTED DECISION COUNT: $executedCount\n".toByteArray())
        stream.write("BRANCH COVERAGE: $branchCoverage%\n".toByteArray())
        stream.write("PER-BRANCH DETAILS:\n".toByteArray())
        decisionResults.forEach { branchId, count ->
            val lineNumber = decisionToLineMap[branchId]?.toString() ?: "???"
            if (count < 2) {
                stream.write("- INCOMPLETE : Decision #$branchId at line #$lineNumber only hit $count time(s).\n".toByteArray())
            } else {
                stream.write("- COMPLETE   : Decision #$branchId at line #$lineNumber hit $count time(s).\n".toByteArray())
            }
        }
        stream.write("QUERY: ($originalStatement)\n".toByteArray())
        stream.write("------------------------\n".toByteArray())
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        stream.flush()
        stream.close()
    }

    // TODO: Implement
    private fun initializeOutputFile(destDir: String, destFile: String) {
        // TODO: Initialize File
        //  val dirPath = Paths.get(destinationDir)
        //  val dir = Files.createDirectories(dirPath)
        //  val file = Files.createFile(dir.resolve(destinationFileName))
        //  println("Absolute file path: ${file.toAbsolutePath()}")
    }

    // TODO: Implement
    private fun initializeXmlFormatter() {
        val str = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?")
            appendLine("<!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"report.dtd\">")
            appendLine("<report name=\"TestBundle\">")
            appendLine("<sessioninfo id=\"SessionId\" start=\"0\" dump=\"10\"/>")

            // Write Package
            appendLine("<package name=\"org/partiql/types\">")

            // Start Class
            appendLine("<class name=\"org/partiql/types/NullType\" sourcefilename=\"StaticType.kt\">")

            // End Class
            appendLine("</class>")

            // Start Class Info
            appendLine("<sourcefile name=\"StaticType.kt\">")

            // End Package
            appendLine("</package>")
            appendLine("</report>")
        }
    }
}
