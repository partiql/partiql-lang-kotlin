package org.partiql.runner

import com.amazon.ion.IonReader
import com.amazon.ion.system.IonReaderBuilder
import java.io.File

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

fun loadReportFile(file: File, dataset: DataSet, commitId: String): Report {
    val report = file.readText()
    return loadReport(report, dataset, commitId)
}

fun loadReport(reportContent: String, dataset: DataSet, commitId: String): Report {
    val reader: IonReader = IonReaderBuilder.standard().build(reportContent)
    val passingSet = mutableSetOf<String>()
    val failingSet = mutableSetOf<String>()
    val ignoredSet = mutableSetOf<String>()

    if (dataset == DataSet.PartiQLCore && !reportContent.contains("partiql-extended")) {
        // Since old report is in old format, we need to read it differently.
        // Read old report to show better view of new report.
        // TODO, remove this after new report in next PR.
        reader.next()
        val result = readTestResult(reader)
        reader.close()
        return Report(dataset, commitId, result)
    } else {
        reader.next()
        reader.stepIn()

        while (reader.next() != null) {
            val tag = reader.fieldName
            if (tag.equals(dataset.dataSetName, ignoreCase = true)) {
                val result = readTestResult(reader)
                reader.close()
                return Report(dataset, commitId, result)
            }
        }
        reader.stepOut()
    }

    // if not found, return empty report
    reader.close()
    return Report(dataset, commitId, TestResult(passingSet, failingSet, ignoredSet))
}

private fun readTestResult(reader: IonReader): TestResult {
    val passingSet = mutableSetOf<String>()
    val failingSet = mutableSetOf<String>()
    val ignoredSet = mutableSetOf<String>()

    reader.stepIn()
    var nextType = reader.next()
    while (nextType != null) {
        when (reader.fieldName) {
            "passing" -> readAll(reader, passingSet)
            "failing" -> readAll(reader, failingSet)
            "ignored" -> readAll(reader, ignoredSet)
        }
        nextType = reader.next()
    }
    reader.stepOut()
    return TestResult(passingSet, failingSet, ignoredSet)
}

private fun readAll(reader: IonReader, mutableList: MutableSet<String>) {
    reader.stepIn()
    var nextType = reader.next()
    while (nextType != null) {
        mutableList.add(reader.stringValue())
        nextType = reader.next()
    }
    reader.stepOut()
}
