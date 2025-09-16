package org.partiql.runner

import com.amazon.ion.IonReader
import com.amazon.ion.system.IonReaderBuilder
import java.io.File

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

fun loadReportFile(file: File, dataSet: DataSet, commitId: String): Report {
    val report = file.readText()
    return loadReport(report, dataSet, commitId)
}

fun loadReport(reportContent: String, dataSet: DataSet, commitId: String): Report {
    val reader: IonReader = IonReaderBuilder.standard().build(reportContent)
    val passingSet = mutableSetOf<String>()
    val failingSet = mutableSetOf<String>()
    val ignoredSet = mutableSetOf<String>()
    reader.next()
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
    reader.close()
    return Report(dataSet, commitId, passingSet.toSet(), failingSet.toSet(), ignoredSet.toSet())
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
