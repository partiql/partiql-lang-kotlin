package org.partiql.runner

data class Report(
    val dataSet: DataSet,
    val commitId: String,
    val testResult: TestResult
) {
    // The short hash
    val commitIdShort = commitId.substring(0..6)
}

enum class DataSet(val dataSetName: String) {
    PartiQLCore("partiql-core"),
    PartiQLExtended("partiql-extended")
}

data class TestResult(
    val passingSet: Set<String> = emptySet(),
    val failingSet: Set<String> = emptySet(),
    val ignoredSet: Set<String> = emptySet()
)
