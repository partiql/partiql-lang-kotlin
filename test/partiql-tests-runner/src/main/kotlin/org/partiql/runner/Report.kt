package org.partiql.runner

data class Report(
    val engine: String,
    val commitId: String,
    // key is dataset name, and value is test result collection.
    var testsResults: MutableMap<String, TestResult> = mutableMapOf()
) {
    data class TestResult(
        val passingSet: Set<String> = emptySet(),
        val failingSet: Set<String> = emptySet(),
        val ignoredSet: Set<String> = emptySet()
    )
    // The short hash
    val commitIdShort = commitId.substring(0..6)
}

enum class DataSet(val dataSetName: String) {
    PartiQL("partiql"),
    PartiQLExtended("partiql-extended")
}
