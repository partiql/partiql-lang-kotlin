package org.partiql.lang.graph

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.graph.SimpleGraph.EdgeDirected
import org.partiql.lang.graph.SimpleGraph.EdgeUndir
import org.partiql.lang.graph.SimpleGraph.Node

object SampleGraphs {
    // NB: The graphs here share nodes and edges.
    // Not sure yet whether this is a feature or something to avoid.

    private val n1 = Node(setOf("a"), ExprValue.newInt(1))

    // single-node no-edges graph:   n1
    val g1: Graph = SimpleGraph(
        nodes = listOf(n1),
        directed = emptyList(),
        undir = emptyList()
    )

    private val e11 = EdgeDirected(setOf("e"), ExprValue.newFloat(1.1))

    // single-node self-loop graph:     --> n1 --
    //                                  |--------|
    val g1L: Graph = SimpleGraph(
        nodes = listOf(n1),
        directed = mapOf(
            e11 to Pair(n1, n1)
        ).toTripleList(),
        undir = emptyList()
    )

    private val n2 = Node(setOf("b"), ExprValue.newInt(2))
    private val e12 = EdgeDirected(setOf("e"), ExprValue.newFloat(1.2))

    //    n1 ---[e12]--> n2
    val g2: Graph = SimpleGraph(
        nodes = listOf(n1, n2),
        directed = mapOf(
            e12 to Pair(n1, n2)
        ).toTripleList(),
        undir = emptyList()
    )

    private val n3 = Node(setOf("a", "b"), ExprValue.newInt(3))
    private val e21 = EdgeDirected(setOf("d"), ExprValue.newFloat(2.1))
    private val e31 = EdgeDirected(setOf("e"), ExprValue.newFloat(3.1))
    private val e23 = EdgeUndir(setOf("e"), ExprValue.newFloat(2.3))

    // a    triangle:     n2 ~~[e23]~~  n3
    //                     \            /
    //                     [e21]       [e31]
    //                        \       /
    //                         v     v
    //                           n1
    val g3: Graph = SimpleGraph(
        nodes = listOf(n1, n2, n3),
        directed = mapOf(
            e21 to Pair(n2, n1),
            e31 to Pair(n3, n1),
        ).toTripleList(),
        undir = mapOf(
            e23 to Pair(n2, n3)
        ).toTripleList()
    )

    // An attempt at convenience/safety machinery:
    //  - Start with [directed] and [undir] as maps and convert them to triple lists before SimpleGraph creations
    //    (to ensure that each edge has only one definition)
    fun <K, V1, V2> Map<K, Pair<V1, V2>>.toTripleList(): List<Triple<V1, K, V2>> =
        this.entries.map { Triple(it.value.first, it.key, it.value.second) }
}
