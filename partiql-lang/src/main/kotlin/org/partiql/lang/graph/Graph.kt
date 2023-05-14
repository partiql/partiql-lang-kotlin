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

    /** Get all the nodes conforming to a label specification. */
    fun scanNodes(spec: LabelSpec): List<Node>

    /** Get undirected edges (and their adjacent nodes) whose labels satisfy the given specifications.
     *  Spec Triple(x, _, y) can be used to compute both patterns (x)~(y) and (y)~(x).
     *  An undirected edge a---b is matched twice, returning x=a, y=b and x=b, y=a.
     */
    fun scanUndir(spec: Triple<LabelSpec, LabelSpec, LabelSpec>): List<Triple<Node, EdgeUndir, Node>>

    /** Get directed edges (and their adjacent nodes) whose labels satisfy the given specifications,
     *  when the requested direction *agrees* with the one at which an edge is defined.
     *  Edge a --> b matches spec Triple(a, _, b),  aka a `--)` b  or b `(--` a,
     *  and gets returned as Triple(a, _ , b). */
    fun scanDirectedStraight(spec: Triple<LabelSpec, LabelSpec, LabelSpec>): List<Triple<Node, EdgeDirected, Node>>

    /** Get directed edges (and their adjacent nodes) whose labels satisfy the given specifications,
     *  when the requested direction is *opposite* to the one at which an edge is defined.
     *  Edge a --> b matches spec Triple(b, _, a),  aka b `--)` a  or a `(--` b,
     *  and gets returned as Triple(b, _ , a).  */
    fun scanDirectedFlipped(spec: Triple<LabelSpec, LabelSpec, LabelSpec>): List<Triple<Node, EdgeDirected, Node>>

    /** Get directed edges without regard for the direction at which they point.
     *  Spec Triple(x, _, y) can be used to compute both patterns (x)<->(y) and (y)<->(x).
     *  A directed edge a --> b is matched twice, returning x=a, y=b and x=b, y=a.
     *
     *  The result of this method can be obtained by combining results of
     *  [scanDirectedStraight] and [scanDirectedFlipped],
     *  but [scanDirectedBlunt] is meant to be implemented with one pass over the data.
     */
    fun scanDirectedBlunt(spec: Triple<LabelSpec, LabelSpec, LabelSpec>): List<Triple<Node, EdgeDirected, Node>>
}

/** Label specifications for selecting graph elements (nodes or edges)
 *  based on labels at them.
 */
sealed class LabelSpec {
    /** A graph element matches when one of its labels is [name].*/
    data class OneOf(val name: String) : LabelSpec()

    /** A graph element always matches (even when it is not labeled at all).*/
    object Whatever : LabelSpec()

    // TODO: more LabelSpec features: alternation, negation, string patterns, ...
}
