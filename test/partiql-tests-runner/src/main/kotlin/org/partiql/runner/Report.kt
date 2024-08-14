package org.partiql.runner

data class Report(
    val engine: String,
    val commitId: String,
    val passingSet: Set<String>,
    val failingSet: Set<String>,
    val ignoredSet: Set<String>
) {
    // The short hash
    val commitIdShort = commitId.substring(0..6)
}