package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.graph.Graph
import org.partiql.lang.graph.SimpleGraph
import org.partiql.lang.graph.SimpleGraph.EdgeDirected
import org.partiql.lang.graph.SimpleGraph.EdgeUndir
import org.partiql.lang.graph.SimpleGraph.Node
import org.partiql.lang.util.ArgumentsProviderBase

/** A few sample graphs constructed directly with a graph API. */
object ApiSampleGraphs {
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

/** The same graphs as above, specified as textual Ion conforming to graph.isl */
object TextSampleGraphs {
    // single-node no-edges graph:   n1
    val g1 = """{ nodes: [ {id: n1, labels: ["a"], payload: 1} ], edges: [] }"""

    // single-node self-loop graph:     --> n1 --[e11]--
    //                                 |---------------|
    val g1L = """{ nodes: [ {id: n1, labels: ["a"], payload: 1} ], 
                |         edges: [ {id: e11, labels: ["e"], payload: 1.1, ends: (n1 -> n1) } ] }""".trimMargin()

    //    n1 ---[e12]--> n2
    val g2 = """{ nodes: [ {id: n1, labels: ["a"], payload: 1},
                |                 {id: n2, labels: ["b"], payload: 2} ], 
                |        edges: [ {id: e12, labels: ["e"], payload: 1.2, ends: (n1 -> n2) } ] }""".trimMargin()

    // a    triangle:     n2 ~~[e23]~~  n3
    //                     \            /
    //                     [e21]       [e31]
    //                        \       /
    //                         v     v
    //                           n1
    val g3 = """{ nodes: [ {id: n1, labels: ["a"], payload: 1},
                |                 {id: n2, labels: ["b"], payload: 2},
                |                 {id: n3, labels: ["a", "b"], payload: 3} ], 
                |        edges: [ {id: e21, labels: ["d"], payload: 2.1, ends: (n2 -> n1) },
                |                 {id: e31, labels: ["e"], payload: 3.1, ends: (n3 -> n1) }, 
                |                 {id: e23, labels: ["e"], payload: 2.3, ends: (n2 -- n3) } ] }""".trimMargin()
}

class GraphsInSessionTests : EvaluatorTestBase() {
    val session = sessionOf(
        mapOf(
            "aG1" to ExprValue.newGraph(ApiSampleGraphs.g1),
            "aG1L" to ExprValue.newGraph(ApiSampleGraphs.g1L),
            "aG2" to ExprValue.newGraph(ApiSampleGraphs.g2),
            "aG3" to ExprValue.newGraph(ApiSampleGraphs.g3),

            "tG1" to graphOfText(TextSampleGraphs.g1),
            "tG1L" to graphOfText(TextSampleGraphs.g1L),
            "tG2" to graphOfText(TextSampleGraphs.g2),
            "tG3" to graphOfText(TextSampleGraphs.g3),

            "rfc" to graphOfResource("graphs/rfc0025-example.ion")
        )
    )

    @ParameterizedTest
    @ArgumentsSource(GraphNames::class)
    fun testGraphReading(graphName: String) {
        runEvaluatorTestCase(
            EvaluatorTestCase(
                query = graphName,
                expectedResult = graphName // just testing the lookup in the session
            ),
            session
        )
    }

    class GraphNames : ArgumentsProviderBase() {
        override fun getParameters(): List<String> =
            listOf(
                "aG1", "aG1L", "aG2", "aG3",
                "tG1", "tG1L", "tG2", "tG3",
                "rfc"
            )
    }
}
