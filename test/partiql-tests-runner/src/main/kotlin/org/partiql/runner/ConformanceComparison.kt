package org.partiql.runner

import com.amazon.ionelement.api.loadSingleElement
import java.io.File

fun main(args: Array<String>) {
    if (args.size < 5) {
        error(
            "Expected at least 5 args: pathToFirstConformanceTestResults, pathToSecondConformanceTestResults" +
                "firstCommitId, secondCommitId, pathToComparisonReport"
        )
    }

    val old = File(args[0])
    val new = File(args[1])

    val oldCommitId = args[2]
    val newCommitId = args[3]

    val oldReports = loadReport(old, oldCommitId)
    val newReports = loadReport(new, newCommitId)
    val comparisonReportFile = File(args[4])
    val limit = if (args.size == 6) args[5].toInt() else Int.MAX_VALUE

    comparisonReportFile.createNewFile()

    // cross engine
    analyze(comparisonReportFile, newReports, limit)

    val all = oldReports + newReports

    // cross commit comparison
    all
        .groupBy { it.engine }
        .forEach { (_, reports) ->
            analyze(comparisonReportFile, reports, limit)
        }
}
data class Report(
    val engine: String,
    val commitId: String,
    val passingSet: Set<String>,
    val failingSet: Set<String>,
    val ignoredSet: Set<String>
)

fun analyze(file: File, reports: List<Report>, limit: Int) {
    var first = 0
    var second = first + 1
    while (first < second && second < reports.size) {
        val report = ReportAnalyzer.build(reports[first], reports[second]).generateComparisonReport(limit)
        file.appendText(report)
        file.appendText("\n")
        if (second < reports.size - 1) {
            second += 1
        } else {
            first += 1
            second = first + 1
        }
    }
}

fun loadReport(dir: File, commitId: String) =
    dir.listFiles()
        ?.filter { it.isDirectory }
        ?.map { sub ->
            val engine = sub.name
            val report =
                (sub.listFiles() ?: throw IllegalArgumentException("sub-dir ${sub.absolutePath} not exist")).first().readText()
            loadReport(report, engine, commitId)
        } ?: throw IllegalArgumentException("dir ${dir.absolutePath} not exist")

fun loadReport(report: String, engine: String, commitId: String): Report {
    val inputStruct = loadSingleElement(report).asStruct()
    val passingSet = inputStruct["passing"].listValues.map { it.stringValue }
    val failingSet = inputStruct["failing"].listValues.map { it.stringValue }
    val ignoredSet = inputStruct["ignored"].listValues.map { it.stringValue }
    return Report(engine, commitId, passingSet.toSet(), failingSet.toSet(), ignoredSet.toSet())
}

abstract class ReportAnalyzer(first: Report, second: Report) {
    companion object {
        fun build(first: Report, second: Report) =
            if (first.engine == second.engine) {
                CrossCommitReportAnalyzer(first, second)
            } else {
                CrossEngineReportAnalyzer(first, second)
            }
    }
    val passingInBoth = first.passingSet.intersect(second.passingSet)
    val failingInBoth = first.failingSet.intersect(second.failingSet)
    val passingFirstFailingSecond = first.passingSet.intersect(second.failingSet)
    val failureFirstPassingSecond = first.failingSet.intersect(second.passingSet)
    val firstPassingSize = first.passingSet.size
    val firstFailingSize = first.failingSet.size
    val firstIgnoreSize = first.ignoredSet.size
    val secondPassingSize = second.passingSet.size
    val secondFailingSize = second.failingSet.size
    val secondIgnoreSize = second.ignoredSet.size

    val firstTotalSize = firstPassingSize + firstFailingSize + firstIgnoreSize
    val secondTotalSize = secondPassingSize + secondFailingSize + secondIgnoreSize

    val firstPassingPercent = firstPassingSize.toDouble() / firstTotalSize * 100
    val secondPassingPercent = secondPassingSize.toDouble() / secondTotalSize * 100

    abstract val reportTitle: String
    abstract fun generateComparisonReport(limit: Int): String
}

class CrossCommitReportAnalyzer(private val first: Report, private val second: Report) :
    ReportAnalyzer(first, second) {
    override val reportTitle: String = "Conformance comparison report-Cross Commit-${first.engine.uppercase()}"
    override fun generateComparisonReport(limit: Int) =
        buildString {
            this.appendLine(
                """### $reportTitle
| | Base (${first.commitId}) | ${second.commitId} | +/- |
| --- | ---: | ---: | ---: |
| % Passing | ${"%.2f".format(firstPassingPercent)}% | ${"%.2f".format(secondPassingPercent)}% | ${"%.2f".format(secondPassingPercent - firstPassingPercent)}% |
| :white_check_mark: Passing | $firstPassingSize | $secondPassingSize | ${secondPassingSize - firstPassingSize} |
| :x: Failing | $firstFailingSize | $secondFailingSize | ${secondFailingSize - firstFailingSize} |
| :large_orange_diamond: Ignored | $firstIgnoreSize | $secondIgnoreSize | ${secondIgnoreSize - firstIgnoreSize} |
| Total Tests | $firstTotalSize | $secondTotalSize | ${secondTotalSize - firstTotalSize} |
                """.trimIndent()
            )
            this.appendLine(
                """
Number passing in both: ${passingInBoth.count()}

Number failing in both: ${failingInBoth.count()}

Number passing in Base (${first.commitId}) but now fail: ${passingFirstFailingSecond.count()}

Number failing in Base (${first.commitId}) but now pass: ${failureFirstPassingSecond.count()}
                """.trimIndent()
            )
            if (passingFirstFailingSecond.isNotEmpty()) {
                // character count limitation with comments in GitHub
                // also, not ideal to list out hundreds of test names
                if (passingFirstFailingSecond.size < limit) {
                    this.appendLine(":interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:. The following test(s) were previously passing but now fail:\n<details><summary>Click here to see</summary>\n\n")

                    passingFirstFailingSecond.forEach { testName ->
                        this.appendLine("- $testName")
                    }
                    this.appendLine("</details>")
                } else {
                    this.appendLine(":interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:")
                    this.appendLine("Download Artifact from Summary to view the complete list")
                }
            }

            if (failureFirstPassingSecond.isNotEmpty()) {
                if (failureFirstPassingSecond.size < limit) {
                    this.appendLine(
                        "The following test(s) were previously failing but now pass. Before merging, confirm they are intended to pass: \n<details><summary>Click here to see</summary>\n\n"
                    )
                    failureFirstPassingSecond.forEach { testName ->
                        this.appendLine("- ${testName}\n")
                    }
                    this.appendLine("</details>")
                } else {
                    this.appendLine("${failureFirstPassingSecond.size} test(s) were previously failing but now pass. Before merging, confirm they are intended to pass")
                    this.appendLine("Download Artifact from Summary to view the complete list")
                }
            }
        }
}

class CrossEngineReportAnalyzer(private val first: Report, private val second: Report) : ReportAnalyzer(first, second) {
    override val reportTitle: String = "Conformance comparison report-Cross Engine"
    override fun generateComparisonReport(limit: Int) =
        buildString {
            this.appendLine(
                """### $reportTitle
| | Base (${first.engine}) | ${second.engine} | +/- |
| --- | ---: | ---: | ---: |
| % Passing | ${"%.2f".format(firstPassingPercent)}% | ${"%.2f".format(secondPassingPercent)}% | ${"%.2f".format(secondPassingPercent - firstPassingPercent)}% |
| :white_check_mark: Passing | $firstPassingSize | $secondPassingSize | ${secondPassingSize - firstPassingSize} |
| :x: Failing | $firstFailingSize | $secondFailingSize | ${secondFailingSize - firstFailingSize} |
| :large_orange_diamond: Ignored | $firstIgnoreSize | $secondIgnoreSize | ${secondIgnoreSize - firstIgnoreSize} |
| Total Tests | $firstTotalSize | $secondTotalSize | ${secondTotalSize - firstTotalSize} |
                """.trimIndent()
            )
            this.appendLine(
                """
Number passing in both: ${passingInBoth.count()}

Number failing in both: ${failingInBoth.count()}

Number passing in ${first.engine} engine but fail in ${second.engine} engine: ${passingFirstFailingSecond.count()}

Number failing in ${first.engine} engine but pass in ${second.engine} engine: ${failureFirstPassingSecond.count()}
                """.trimIndent()
            )
            if (passingFirstFailingSecond.isNotEmpty()) {
                if (passingFirstFailingSecond.size < limit) {
                    this.appendLine(":interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:. The following test(s) are passing in ${first.engine} but fail in ${second.engine}:\n<details><summary>Click here to see</summary>\n\n")

                    passingFirstFailingSecond.forEach { testName ->
                        this.appendLine("- $testName")
                    }
                    this.appendLine("</details>")
                } else {
                    this.appendLine(":interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:")
                    this.appendLine("Download Artifact from Summary to view the complete list")
                }
            }

            if (failureFirstPassingSecond.isNotEmpty()) {
                if (failureFirstPassingSecond.size < 10) {
                    this.appendLine(
                        "The following test(s) are failing in ${first.engine} but pass in ${second.engine}. Before merging, confirm they are intended to pass: \n<details><summary>Click here to see</summary>\n\n"
                    )
                    failureFirstPassingSecond.forEach { testName ->
                        this.appendLine("- ${testName}\n")
                    }
                    this.appendLine("</details>")
                } else {
                    this.appendLine("${failureFirstPassingSecond.size} test(s) were failing in ${first.engine} but now pass in ${second.engine}. Before merging, confirm they are intended to pass.")
                    this.appendLine("Download Artifact from Summary to view the complete list")
                }
            }
        }
}
