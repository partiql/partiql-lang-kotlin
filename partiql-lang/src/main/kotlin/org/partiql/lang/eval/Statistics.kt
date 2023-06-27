package org.partiql.lang.eval

data class Statistics(
    val decisionCount: Int,
    val executedDecisions: Map<Int, Set<Boolean>> = emptyMap(),
    val locations: Map<Int, Int>
)
