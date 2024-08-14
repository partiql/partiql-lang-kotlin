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
