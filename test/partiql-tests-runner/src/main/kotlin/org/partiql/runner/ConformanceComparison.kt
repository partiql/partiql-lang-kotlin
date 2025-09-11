package org.partiql.runner

import com.amazon.ion.IonReader
import com.amazon.ion.system.IonReaderBuilder
import java.io.File
import kotlin.collections.mutableMapOf

fun analyze(file: File, reports: List<Report>, limit: Int, title: String) {
    var first = 0
    var second = first + 1
    while (first < second && second < reports.size) {
        val report = ReportAnalyzer.build(title, reports[first], reports[second]).generateComparisonReport(limit)
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

fun loadReportFile(file: File, engine: String, commitId: String): Report {
    val report = file.readText()
    return loadReport(report, engine, commitId)
}

fun loadReport(reportContent: String, engine: String, commitId: String): Report {
    val reader: IonReader = IonReaderBuilder.standard().build(reportContent)
    val report = Report(engine, commitId, mutableMapOf())

    if (!reportContent.contains("partiql-extended")){
        // Since old report is in old format, we need to read it differently.
        // Read old report to show better view of new report.
        // TODO, remove this after new report in next PR.
        reader.next()
        readTestResult(reader).let { report.testsResults["partiql"] = it }
    } else {
        reader.next()
        reader.stepIn()

        while (reader.next() != null) {
            val tag = reader.fieldName
            readTestResult(reader).let { report.testsResults[tag] = it }
        }
        reader.stepOut()
    }
    reader.close()
    return report
}

private fun readTestResult(reader: IonReader): Report.TestResult {
    val result = Report.TestResult()

    reader.stepIn()
    var nextType = reader.next()
    while (nextType != null) {
        when (reader.fieldName) {
            "passing" -> readAll(reader, result.passingSet)
            "failing" -> readAll(reader, result.failingSet)
            "ignored" -> readAll(reader, result.ignoredSet)
        }
        nextType = reader.next()
    }
    reader.stepOut()
    return result
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
