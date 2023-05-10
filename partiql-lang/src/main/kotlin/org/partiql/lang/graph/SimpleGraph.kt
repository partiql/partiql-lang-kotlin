package org.partiql.lang.graph

import org.partiql.lang.eval.ExprValue

internal typealias Labels = Set<String>

/** A straightforward implementation of in-memory graphs. */
class SimpleGraph(
    val nodes: List<Node>,
    val directed: List<Triple<Node, EdgeDirected, Node>>,
    val undir: List<Triple<Node, EdgeUndir, Node>> // order of Nodes in the Triple "doesn't matter"
) : Graph {

    // Intentionally, not a data class -- want to use pointer equality
    class Node(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Graph.Node

    abstract class Edge : Graph.Edge

    class EdgeDirected(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Edge(), Graph.EdgeDirected

    class EdgeUndir(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Edge(), Graph.EdgeUndir
}
