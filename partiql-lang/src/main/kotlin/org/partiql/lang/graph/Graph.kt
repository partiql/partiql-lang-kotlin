package org.partiql.lang.graph

import org.partiql.lang.eval.ExprValue

/** This is an "external" interface to a graph data value,
 *  providing functionality needed for a pattern-matching processor.
 *  The intent is to come up with something that can be implemented by different "platforms"
 *  in different ways.
 *  There are only minimal assumptions about the underlying implementation of
 *  graph nodes and edges: they must provide access to labels and payloads
 *  and the == equality must distinguish and equate them properly.
 *  In particular, there is no node-edge-node "pointer" navigation.
 *  The graph's structure is exposed only through the "scan" functions for getting
 *  adjacent nodes and edges satisfying certain criteria.
 *
 *  TODO:
 *   - Expand "criteria" beyond label specs, to include predicates ("prefilters"),
 *     perhaps as call-back lambdas.
 *   - Instead of returning results as [List]s, consider something lazy/streaming, perhaps [Sequence].
 */
interface Graph {

    interface Elem {
        val labels: Set<String>
        val payload: ExprValue
    }

    interface Node : Elem
    interface Edge : Elem
    interface EdgeDirected : Edge
    interface EdgeUndir : Edge
}
