package org.partiql.lang.graph

import org.partiql.lang.eval.ExprValue

typealias Labels = Set<String>

/** A straightforward implementation of in-memory graphs. */
internal class SimpleGraph(
    internal val nodes: List<Node>,
    internal val directed: List<Triple<Node, EdgeDirected, Node>>,
    internal val undir: List<Triple<Node, EdgeUndir, Node>> // order of Nodes in the Triple "doesn't matter"
) : Graph {

    // Intentionally, not a data class -- want to use pointer equality
    internal class Node(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Graph.Node

    internal abstract class Edge : Graph.Edge

    internal class EdgeDirected(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Edge(), Graph.EdgeDirected

    internal class EdgeUndir(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Edge(), Graph.EdgeUndir
}
