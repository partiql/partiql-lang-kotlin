package org.partiql.lang.graph

import org.partiql.lang.eval.ExprValue

typealias Labels = Set<String>

/** A straightforward implementation of in-memory graphs. */
internal class SimpleGraph(
    private val nodes: List<Node>,
    private val directed: List<Triple<Node, EdgeDirected, Node>>,
    private val undir: List<Triple<Node, EdgeUndir, Node>> // order of Nodes in the Triple "doesn't matter"
) : Graph {

    // Intentionally, not a data class -- want to use pointer equality
    internal class Node(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Graph.Node

    internal class EdgeDirected(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Graph.EdgeDirected

    internal class EdgeUndir(
        override val labels: Labels,
        override val payload: ExprValue
    ) : Graph.EdgeUndir

    private fun labelsMatchSpec(labels: Set<String>, spec: LabelSpec): Boolean =
        when (spec) {
            LabelSpec.Whatever -> true
            is LabelSpec.OneOf -> labels.contains(spec.name)
        }

    private fun Graph.Elem.matches(labelSpec: LabelSpec): Boolean =
        labelsMatchSpec(this.labels, labelSpec)

    override fun getNodes(spec: LabelSpec): List<Node> {
        return nodes.filter { it.matches(spec) }
    }

    override fun getDirectedStraight(spec: Triple<LabelSpec, LabelSpec, LabelSpec>): List<Triple<Node, EdgeDirected, Node>> {
        val (srcSpec, edgeSpec, dstSpec) = spec
        return directed.filter {
            val (src, edge, dst) = it
            src.matches(srcSpec) && edge.matches(edgeSpec) && dst.matches(dstSpec)
        }
    }

    override fun getDirectedFlipped(spec: Triple<LabelSpec, LabelSpec, LabelSpec>): List<Triple<Node, EdgeDirected, Node>> {
        val (srcSpec, edgeSpec, dstSpec) = spec
        return directed.asSequence().filter {
            val (dst, edge, src) = it // data triple flipped, for filtering
            src.matches(srcSpec) && edge.matches(edgeSpec) && dst.matches(dstSpec)
        }.map { Triple(it.third, it.second, it.first) }.toList() // flipped agan, for the result
    }

    private fun <E : Graph.Edge> getBlunt(
        triples: List<Triple<Node, E, Node>>,
        spec: Triple<LabelSpec, LabelSpec, LabelSpec>
    ): List<Triple<Node, E, Node>> {
        val (srcSpec, edgeSpec, dstSpec) = spec
        val selected = mutableListOf<Triple<Node, E, Node>>()
        for (t in triples) {
            val (src, edge, dst) = t
            if (edge.matches(edgeSpec)) {
                if (src.matches(srcSpec) && dst.matches(dstSpec))
                    selected.add(t)
                if ((src.matches(dstSpec) && dst.matches(srcSpec)))
                    selected.add(Triple(dst, edge, src))
            }
        }
        return selected.toList()
    }

    override fun getDirectedBlunt(spec: Triple<LabelSpec, LabelSpec, LabelSpec>): List<Triple<Node, EdgeDirected, Node>> =
        getBlunt(directed, spec)

    override fun getUndir(spec: Triple<LabelSpec, LabelSpec, LabelSpec>): List<Triple<Node, EdgeUndir, Node>> =
        getBlunt(undir, spec)
}
