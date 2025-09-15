package org.partiql.runner

data class Report(
    val dataSet: DataSet,
    val commitId: String,
    val testResult: TestResult
)

enum class DataSet(val dataSetName: String) {
    PartiQLCore("partiql-core"),
    PartiQLExtended("partiql-extended")
}

data class TestResult(
    val passingSet: Set<String> = emptySet(),
    val failingSet: Set<String> = emptySet(),
    val ignoredSet: Set<String> = emptySet()
)
