package org.partiql.lang.graph

import com.amazon.ion.IonList
import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionschema.IonSchemaSystemBuilder
import com.amazon.ionschema.Schema
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.partiqlisl.getResourceAuthority
import java.io.File

class GraphValidationException(message: String) : RuntimeException(message)
class GraphReadException(message: String) : RuntimeException(message)

internal typealias EdgeTriple<E> = Triple<SimpleGraph.Node, E, SimpleGraph.Node>

/** A validator and reader for external graphs represented in Ion in accordance with the graph.isl schema.
 */
object ExternalGraphReader {

    // Constants for the graph schema and names used inside it.
    private const val islSchemaFile = "graph.isl"
    private const val islGraph = "Graph"

    private val ion: IonSystem = IonSystemBuilder.standard().build()
    private val iss = IonSchemaSystemBuilder.standard()
        .addAuthority(getResourceAuthority(ion))
        .withIonSystem(ion)
        .build()
    private val graphSchema: Schema = iss.loadSchema(islSchemaFile)
    private val graphType = graphSchema.getType(islGraph)
        ?: error("Definition for type $islGraph not found in ISL schema $islSchemaFile")

    /** Validates an IonValue to be a graph according to the graph.isl ISL schema. */
    fun validate(graphIon: IonValue) {
        val violations = graphType.validate(graphIon)
        if (!violations.isValid())
            throw GraphValidationException("Ion data did not validate as a graph: \n$violations")
    }

    fun validate(graphStr: String) {
        val graphIon = ion.singleValue(graphStr)
        validate(graphIon)
    }

    fun validate(graphFile: File) {
        val graphStr = graphFile.readText()
        validate(graphStr)
    }

    /** Given an IonValue, validates that it is a graph in accordance with graph.isl schema
     *  and loads it into memory as a SimpleGraph. */
    internal fun read(graphIon: IonValue): SimpleGraph {
        validate(graphIon)
        return readGraph(graphIon)
    }
    internal fun read(graphStr: String): SimpleGraph {
        val graphIon = ion.singleValue(graphStr)
        validate(graphIon)
        return readGraph(graphIon)
    }

    /* Entry point into the implementation of graph reading.
     * It is assumed that the input Ion value has been validated w.r.t. graph ISL,
     * so that defensive error checks are not needed.
     */
    private fun readGraph(graphIon: IonValue): SimpleGraph {
        val g = graphIon as IonStruct
        val nds = g.get("nodes") as IonList
        val eds = g.get("edges") as IonList
        val nodes = readNodes(nds)
        val (directed, undirected) = EdgeReader(nodes).readEdges(eds)
        return SimpleGraph(nodes.values.toList(), directed, undirected)
    }

    /* Returns a map from node IDs in the source graph to newly-built in-memory nodes.
    *  This map is used later to recognize node IDs used in edge definitions.
    */
    private fun readNodes(nodesList: IonList): Map<String, SimpleGraph.Node> {
        val pairs = nodesList.toList().map { readNode(it as IonStruct) }
        val duplicates = pairs.map { it.first }.groupBy { it }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) throw GraphReadException(
            "Identifiers used for more than one node: ${duplicates.joinToString()}."
        )
        return pairs.toMap()
    }

    private fun readNode(node: IonStruct): Pair<String, SimpleGraph.Node> {
        val (id, labels, payload) = readCommon(node)
        return Pair(id, SimpleGraph.Node(labels, payload))
    }

    /*  Can be called on an IonStruct for either a graph node or an edge,
     *  to extract their components that are structured identically: id, labels, payload.  */
    private fun readCommon(node: IonStruct): Triple<String, Set<String>, ExprValue> {
        val id = (node.get("id")!! as IonSymbol).symbolValue().assumeText()
        val lbs = node.get("labels")
        val labels =
            if (lbs == null) { emptySet() } else { (lbs as IonList).toList().map { (it as IonString).stringValue() }.toSet() }
        val pld = node.get("payload")
        val payload =
            if (pld == null) { ExprValue.nullValue } else ExprValue.of(pld)
        return Triple(id, labels, payload)
    }

    /* EdgeReader class is a scoping trick, to avoid threading the [allNodes] argument through the remaining functions. */
    private class EdgeReader(val allNodes: Map<String, SimpleGraph.Node>) {

        internal fun readEdges(edgesList: IonList): Pair<List<EdgeTriple<SimpleGraph.EdgeDirected>>, List<EdgeTriple<SimpleGraph.EdgeUndir>>> {
            val dirs = mutableListOf<EdgeTriple<SimpleGraph.EdgeDirected>>()
            val undirs = mutableListOf<EdgeTriple<SimpleGraph.EdgeUndir>>()
            val triples = edgesList.toList().map { readEdge(it as IonStruct) }
            triples.forEach {
                val (n1, e, n2) = it
                when (e) {
                    is SimpleGraph.EdgeDirected -> dirs += Triple(n1, e, n2)
                    is SimpleGraph.EdgeUndir -> undirs += Triple(n1, e, n2)
                }
            }
            return Pair(dirs.toList(), undirs.toList())
        }

        internal fun readEdge(edge: IonStruct): EdgeTriple<SimpleGraph.Edge> {
            val (id, labels, payload) = readCommon(edge)
            val ends = edge.get("ends")!! as IonSexp
            val n1 = endNode(ends, 0)
            val marker = (ends.get(1) as IonSymbol).symbolValue().assumeText()
            val n2 = endNode(ends, 2)
            return when (marker) {
                "--", "---" -> Triple(n1, SimpleGraph.EdgeUndir(labels, payload), n2)
                "->", "-->" -> Triple(n1, SimpleGraph.EdgeDirected(labels, payload), n2)
                "<-", "<--" -> Triple(n2, SimpleGraph.EdgeDirected(labels, payload), n1) // flip
                else -> throw GraphReadException(
                    "BUG: At edge $id, directionality marker not recognized: $marker"
                )
            }
        }

        internal fun endNode(seq: IonSequence, idx: Int): SimpleGraph.Node {
            val id = (seq.get(idx) as IonSymbol).symbolValue().assumeText()
            return allNodes.getOrElse(id) {
                throw GraphReadException("Node id $id is used in an edge, but it was not defined as a node")
            }
        }
    }
}
