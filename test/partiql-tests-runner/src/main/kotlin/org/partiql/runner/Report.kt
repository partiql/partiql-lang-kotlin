package org.partiql.runner

data class Report(
    val engine: String,
    val commitId: String,
    var testsResults: MutableMap<String,TestResult> = mutableMapOf()
) {
    data class TestResult(
        var passingSet: MutableSet<String> = mutableSetOf(),
        var failingSet: MutableSet<String> = mutableSetOf(),
        var ignoredSet: MutableSet<String> = mutableSetOf()
    )
    // The short hash
    val commitIdShort = commitId.substring(0..6)
}
