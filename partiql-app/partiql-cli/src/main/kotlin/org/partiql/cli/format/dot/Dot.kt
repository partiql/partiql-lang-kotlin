/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.cli.format.dot

import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * This file is copied from https://github.com/RCHowell/Dotlin, however the Maven artifact only builds with Java 18+,
 * therefore, the functionality is copied here. If Dotlin (io.github.rchowell:dotlin:1.0.1) updates their release to
 * allow Java 8+, we can just use the open-source library.
 */

const val INDENT = "\t"

/**
 * graph { ... }
 */
fun graph(name: String? = null, strict: Boolean = false, f: DotRootGraph.() -> Unit): DotRootGraph {
    val graph = DotRootGraph(name, strict, DotEdgeOp.UNDIR)
    graph.f()
    return graph
}

/**
 * digraph { ... }
 */
fun digraph(name: String? = null, strict: Boolean = false, f: DotRootGraph.() -> Unit): DotRootGraph {
    val graph = DotRootGraph(name, strict, DotEdgeOp.DIR)
    graph.f()
    return graph
}

/**
 * graph: [ strict ] (graph | digraph) [ ID ] '{' stmt_list '}'
 */
sealed class DotGraph(
    val name: String?,
    val strict: Boolean,
    val edgeOp: DotEdgeOp
) : DotAttrCapture() {

    /**
     * stmt_list: [ stmt [ ';' ] stmt_list ]
     */
    val stmts = mutableListOf<DotStmt>()

    /**
     * Node attribute statement
     * - Dot: node [style=filled,color=white];
     * - DSL: node {
     *     style = "filled"
     *     color = "white"
     *   }
     */
    inline fun node(f: DotNodeAttrStmt.() -> Unit) {
        val stmt = DotNodeAttrStmt(true)
        stmt.f()
        stmts.add(stmt)
    }

    /**
     * Subgraph Dot entity with direction inherited from parent Graph
     */
    inline fun subgraph(name: String? = null, f: DotSubgraph.() -> Unit): DotSubgraph {
        val stmt = DotSubgraph(name, edgeOp)
        stmt.f()
        return stmt
    }

    /**
     * Adding a subgraph to the root graph requires "+" just like nodes
     */
    operator fun DotSubgraph.unaryPlus() {
        this@DotGraph.stmts.add(this)
    }

    /**
     * Node statement is a "+" with an identifier string.
     * This aligns with the HTML Kotlin DSL
     */
    operator fun String.unaryPlus(): DotNodeStmt {
        val stmt = DotNodeStmt(DotNodeId(this))
        stmts.add(stmt)
        return stmt
    }

    operator fun DotNodeStmt.unaryPlus(): DotNodeStmt {
        stmts.add(this)
        return this
    }

    /**
     * I couldn't figure out how to chain edges while also attaching edge attributes
     *  hence why these return a org.partiql.cli.format.DotEdgeStmt and not the rhs
     */

    /**
     * Node to Node
     */
    infix operator fun String.minus(target: String): DotEdgeStmt {
        val lhs = DotNodeId(this)
        val rhs = DotNodeId(target)
        val stmt = when (edgeOp) {
            DotEdgeOp.DIR -> DotEdgeStmt(DotEdge.NodeDirNode(lhs, rhs))
            DotEdgeOp.UNDIR -> DotEdgeStmt(DotEdge.NodeUnDirNode(lhs, rhs))
        }
        stmts.add(stmt)
        return stmt
    }

    /**
     * Node to Subgraph
     */
    infix operator fun String.minus(target: DotSubgraph): DotEdgeStmt {
        val lhs = DotNodeId(this)
        val stmt = when (edgeOp) {
            DotEdgeOp.DIR -> DotEdgeStmt(DotEdge.NodeDirSubgraph(lhs, target))
            DotEdgeOp.UNDIR -> DotEdgeStmt(DotEdge.NodeUnDirSubgraph(lhs, target))
        }
        this@DotGraph.stmts.add(stmt)
        return stmt
    }

    /**
     * Subgraph to Node
     */
    infix operator fun DotSubgraph.minus(target: String): DotEdgeStmt {
        val rhs = DotNodeId(target)
        val stmt = when (edgeOp) {
            DotEdgeOp.DIR -> DotEdgeStmt(DotEdge.SubgraphDirNode(this, rhs))
            DotEdgeOp.UNDIR -> DotEdgeStmt(DotEdge.SubgraphUnDirNode(this, rhs))
        }
        this@DotGraph.stmts.add(stmt)
        return stmt
    }

    /**
     * Subgraph to Subgraph
     */
    infix operator fun DotSubgraph.minus(target: DotSubgraph): DotEdgeStmt {
        val stmt = when (edgeOp) {
            DotEdgeOp.DIR -> DotEdgeStmt(DotEdge.SubgraphDirSubgraph(this, target))
            DotEdgeOp.UNDIR -> DotEdgeStmt(DotEdge.SubgraphUnDirSubgraph(this, target))
        }
        this@DotGraph.stmts.add(stmt)
        return stmt
    }
}

class DotRootGraph(
    name: String?,
    strict: Boolean,
    edgeOp: DotEdgeOp
) : DotGraph(name, strict, edgeOp) {

    var background: String? by attr("_background")

    var bb: DotRect? by attr()

    var bgcolor: String? by attr()

    var center: Boolean? by attr()

    var charset: String? by attr()

    var clazz: String? by attr("class")

    var clusterrank: DotClusterMode? by attr()

    var colorscheme: String? by attr()

    var comment: String? by attr()

    var compound: Boolean? by attr()

    var concentrate: Boolean? by attr()

    var damping: Double? by attr("Damping")

    var defaultdist: Double? by attr()

    var dim: Int? by attr()

    var dimen: Int? by attr()

    var diredgecontraintsS: String? by attr("diredgeconstraints")

    var diredgeconstraintsB: Boolean? by attr("diredgeconstraints")

    var dpi: Double? by attr()

    var epsilon: Double? by attr()

    var esep: Double? by attr()

    var fontcolor: String? by attr()

    var fontname: String? by attr()

    var fontnames: String? by attr()

    var fontpath: String? by attr()

    var fontsize: Double? by attr()

    var forcelabels: Boolean? by attr()

    var gradientangle: Int? by attr()

    var href: String? by attr()

    var id: String? by attr()

    var imagepath: String? by attr()

    var inputscale: Double? by attr()

    var k: Double? by attr("K")

    var labelscheme: Int? by attr("label_scheme")

    var labeljust: String? by attr()

    var labelloc: String? by attr()

    var landscape: Boolean? by attr()

    var layerlistsep: String? by attr()

    var layers: String? by attr()

    var layout: String? by attr()

    var levels: Int? by attr()

    var levelsgap: Double? by attr()

    var lheight: Double? by attr()

    var lp2: Pair<Double, Double>? by attr("lp")

    var lp3: Triple<Double, Double, Double>? by attr("lp")

    var lwidth: Double? by attr()

    var margin: Double? by attr()

    var marginP: Pair<Double, Double>? by attr("margin")

    var marginP3: Triple<Double, Double, Double>? by attr("margin")

    var maxiter: Int? by attr()

    var mclimit: Double? by attr()

    var mindist: Double? by attr()

    var mode: String? by attr()

    var model: String? by attr()

    var mosek: Boolean? by attr()

    var newrank: Boolean? by attr()

    var nodesep: Double? by attr()

    var normalizeB: Boolean? by attr("normalize")

    var normalizeD: Double? by attr("normalize")

    var notranslate: Boolean? by attr()

    var nslimit: Double? by attr()

    var nslimit1: Double? by attr()

    var ordering: String? by attr()

    var orientationB: Boolean? by attr("orientation")

    var orientationS: String? by attr("orientation")

    var outputorder: DotOutputMode? by attr()

    var overlapS: String? by attr("overlap")

    var overlapB: Boolean? by attr("overlap")

    var overlapScaling: Double? by attr("overlap_scaling")

    var overlapShrink: Boolean? by attr("overlap_shrink")

    var packB: Boolean? by attr("pack")

    var packI: Int? by attr("pack")

    var packmode: String? by attr()

    var pad: Double? by attr()

    var page: Double? by attr()

    var pagdir: DotPageDir? by attr()

    var quadtree: DotQuadType? by attr()

    var quantum: Double? by attr()

    var rankdir: DotRankDir? by attr()

    var ranksep: Double? by attr()

    var ratio: Double? by attr()

    var ratioS: String? by attr("ratio")

    var ratioD: Double? by attr("ratio")

    var remincross: Boolean? by attr()

    var repulsiveforce: Double? by attr()

    var resolution: Double? by attr()

    var rootS: String? by attr("root")

    var rootB: Boolean? by attr("root")

    var rotate: Int? by attr()

    var rotation: Double? by attr()

    var scale: Double? by attr()

    var scaleP2: Pair<Double, Double>? by attr("scale")

    var scaleP3: Triple<Double, Double, Double>? by attr("scale")

    var searchsize: Int? by attr()

    var sep: Double? by attr()

    var sepP: Pair<Double, Double>? by attr("sep")

    var showboxes: Int? by attr()

    var size: Double? by attr()

    var sizeP2: Pair<Double, Double>? by attr("size")

    var sizeP3: Triple<Double, Double, Double>? by attr("size")

    var smoothing: DotSmoothType? by attr()

    var sortv: Int? by attr()

    var splines: Boolean? by attr()

    var splinesS: String? by attr("splines")

    var start: String? by attr()

    var stylesheet: String? by attr()

    var target: String? by attr()

    var truecolor: Boolean? by attr()

    var url: String? by attr("URL")

    var viewport: String? by attr()

    var voroMargin: Double? by attr("voro_margin")

    var xdotversion: String? by attr()

    /**
     * Returns the Dot code for this graph
     */
    fun dot(): String = with(StringBuilder()) {
        if (strict) append("strict ")
        if (edgeOp == DotEdgeOp.UNDIR) append("graph ") else append("digraph ")
        if (name != null) append("$name ")
        appendLine("{")
        if (attrs.isNotEmpty()) {
            appendLine("$INDENT// Attributes")
            appendLine(attrs.dot(1, "\n"))
            appendLine("$INDENT// Statements")
        }
        stmts.forEach { stmt ->
            stmt.dot(this, 1)
            append("\n")
        }
        appendLine("}")
        toString()
    }
}

/**
 * subgraph: [ subgraph [ ID ] ] '{' stmt_list '}'
 */
class DotSubgraph(name: String?, edgeOp: DotEdgeOp) : DotGraph(name, false, edgeOp), DotVertex, DotStmt {

    var area: Double? by attr()

    var bgcolor: String? by attr()

    var clazz: String? by attr()

    var colorscheme: String? by attr()

    var fillcolor: String? by attr()

    var layer: String? by attr()

    var peripheries: String? by attr()

    var tooltip: String? by attr()

    var rank: String? by attr()

    var color: String? by attr()

    var style: DotSubgraphStyle? by attr()

    var label: String? by attr()

    override fun dot(sb: StringBuilder, indent: Int, leadingWhitespace: Boolean): Unit = with(sb) {
        val prefix = INDENT.repeat(indent)
        val prefix1 = prefix + INDENT
        if (leadingWhitespace) append(prefix)
        if (name != null) appendLine("subgraph $name {") else appendLine("subgraph {")
        if (attrs.isNotEmpty()) {
            appendLine("$prefix1// Attributes")
            appendLine(attrs.dot(indent + 1, "\n"))
            appendLine("$prefix1// Statements")
        }
        stmts.forEach { stmt ->
            stmt.dot(this, indent + 1)
            append("\n")
        }
        append(INDENT.repeat(indent)).append("}")
    }

    /**
     * Subgraph can also be an entity. Same code but with leading whitespace.
     */
    override fun dot(sb: StringBuilder, indent: Int) = dot(sb, indent, true)
}

/**
 * Interface for Dot generating
 */
interface DotEntity {

    /**
     * Entity adds its Dot code to the StringBuilder
     */
    fun dot(sb: StringBuilder, indent: Int = 0)
}

/**
 * Calling a org.partiql.cli.format.DotVertex entities that can be the source or target of an edge -- i.e. node ids and subgraphs
 * This affects indentation in Dot generating.
 */
interface DotVertex {

    fun dot(sb: StringBuilder, indent: Int, leadingWhitespace: Boolean = false)
}

/**
 * stmt: node_stmt
 * | edge_stmt
 * | attr_stmt
 * | ID '=' ID
 * | subgraph
 */
interface DotStmt : DotEntity

/**
 *  node_stmt: node_id [ attr_list ]
 */
class DotNodeStmt(private val nodeId: DotNodeId) : DotStmt {

    /**
     * Attributes for this node
     */
    private val attrStmt = DotNodeAttrStmt()

    /**
     * Adds attributes to this node statement
     */
    infix operator fun plus(f: DotNodeAttrStmt.() -> Unit) = attrStmt.f()

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append(INDENT.repeat(indent))
        append(nodeId.id)
        attrStmt.dot(this, indent)
    }
}

/**
 * node_id: ID [ port ]
 */
class DotNodeId(val id: String, private val port: DotPortPos? = null) : DotVertex {
    override fun dot(sb: StringBuilder, indent: Int, leadingWhitespace: Boolean): Unit = with(sb) {
        if (port == null) {
            append(id)
        } else {
            append("$id $port")
        }
    }
}

/**
 *  Supported
 *  a - b
 *  a - c
 *
 *  Not Supported
 *  a - b - c
 *
 * edge_stmt : (node_id | subgraph) edgeRHS [ attr_list ]
 * edgeRHS   : edgeop (node_id | subgraph) [ edgeRHS ]
 *
 */
class DotEdgeStmt(private val edge: DotEdge) : DotStmt {

    /**
     * Attributes for this edge
     */
    private val attrStmt = DotEdgeAttrStmt()

    infix operator fun plus(f: DotEdgeAttrStmt.() -> Unit) {
        attrStmt.f()
    }

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append(INDENT.repeat(indent))
        edge.dot(sb, indent)
        attrStmt.dot(sb, indent)
    }
}

/**
 * -> in directed graphs
 * -- in undirected graphs
 */
enum class DotEdgeOp(val v: String) {
    DIR("->"),
    UNDIR("--");

    override fun toString(): String = v
}

/**
 * Eight variants of a node connection
 */
sealed class DotEdge(
    private val from: DotVertex,
    private val to: DotVertex,
    private val op: DotEdgeOp,
) : DotEntity {

    class NodeUnDirNode(from: DotNodeId, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class NodeDirNode(from: DotNodeId, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.DIR)

    class SubgraphUnDirSubgraph(from: DotSubgraph, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class SubgraphDirSubgraph(from: DotSubgraph, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.DIR)

    class NodeUnDirSubgraph(from: DotNodeId, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class NodeDirSubgraph(from: DotNodeId, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.DIR)

    class SubgraphUnDirNode(from: DotSubgraph, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class SubgraphDirNode(from: DotSubgraph, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.DIR)

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        from.dot(this, indent)
        append(" $op ")
        to.dot(this, indent)
    }
}

/**
 * Extension function to print a set of key values in the Dot language
 */
fun MutableMap<String, Any>.dot(indent: Int = 0, separator: String = ","): String {
    val prefix = INDENT.repeat(indent)
    return this@dot.map { (k, v) ->
        val valueString = when (v) {
            is String -> "\"$v\"" // strings are always quoted. Might be problematic for escString type such as HTML
            is Pair<*, *> -> "\"${v.first},${v.second}!\""
            is Triple<*, *, *> -> "\"${v.first},${v.second},${v.third}!\""
            else -> v.toString()
        }
        "$prefix$k=$valueString"
    }.joinToString(separator) { it }
}

/**
 * This has turned into generic logic for saving non-null object properties with a key override
 */
abstract class DotAttrCapture {

    var attrs = mutableMapOf<String, Any>()

    /**
     * Inspired by the Delegate.observable
     */
    fun <T> attr(nameOverride: String? = null): ReadWriteProperty<Any?, T?> = object : ObservableProperty<T?>(null) {
        override fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?) {
            val name = nameOverride ?: property.name
            if (newValue == null) {
                attrs.remove(name)
            } else {
                attrs[name] = newValue
            }
        }
    }
}

/**
 * Dot attributes. http://www.graphviz.org/doc/info/attrs.html
 * attr_stmt: (graph | node | edge) attr_list
 */
sealed class DotAttrStmt : DotStmt, DotAttrCapture() {

    /**
     * Attribute statements not associated with a particular graph, node, or edge
     */
    abstract val standalone: Boolean

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        if (standalone) {
            append(INDENT.repeat(indent))
            append(
                when (this@DotAttrStmt) {
                    is DotNodeAttrStmt -> "node"
                    is DotEdgeAttrStmt -> "edge"
                }
            )
        }
        if (attrs.isEmpty()) {
            // Only print empty brackets on a standalone attribute statement
            if (standalone) append("[]")
            return
        }
        append("[").append(attrs.dot()).append("]")
    }
}

/**
 * Node specific attribute statement.
 * All functions are node attributes.
 */
class DotNodeAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    var area: Double? by attr()

    var clazz: String? by attr("class")

    var color: String? by attr()

    var colorscheme: String? by attr()

    var comment: String? by attr()

    var distortion: Double? by attr()

    var fillcolor: String? by attr()

    var fixedsize: Boolean? by attr()

    var fixedsizeS: String? by attr("fixedsize")

    var fontcolor: String? by attr()

    var fontname: String? by attr()

    var gradientangle: Int? by attr()

    var group: String? by attr()

    var height: Double? by attr()

    var href: Double? by attr()

    var id: String? by attr()

    var image: String? by attr()

    var imagepos: String? by attr()

    var imagescale: Boolean? by attr()

    var imagescaleS: String? by attr("imagescale")

    var label: String? by attr()

    var labelloc: String? by attr()

    var layer: String? by attr()

    var margin: Double? by attr()

    var marginP2: Pair<Double, Double>? by attr("margin")

    var marginP3: Triple<Double, Double, Double>? by attr("margin")

    var ordering: String? by attr()

    var orientationB: Boolean? by attr("orientation")

    var orientationS: String? by attr("orientation")

    var penwidth: Double? by attr()

    var peripheries: Int? by attr()

    var pin: Boolean? by attr()

    var pos2: Pair<Double, Double>? by attr("pos")

    var pos3: Triple<Double, Double, Double>? by attr("pos")

    var rects: DotRect? by attr()

    var regular: Boolean? by attr()

    var rootS: String? by attr("root")

    var rootB: Boolean? by attr("root")

    var samplepoints: Int? by attr()

    var shape: DotNodeShape? by attr()

    var shapefile: String? by attr()

    var showboxes: Int? by attr()

    var sides: Int? by attr()

    var skew: Double? by attr()

    var sortv: Int? by attr()

    var style: String? by attr()

    var target: String? by attr()

    var tooltip: String? by attr()

    var url: String? by attr("URl")

    var vertices: String? by attr()

    var width: Double? by attr()

    var xlabel: String? by attr()

    var xlp2: Pair<Double, Double>? by attr("xlp")

    var xlp3: Triple<Double, Double, Double>? by attr("xlp")

    var z: Double? by attr()
}

/**
 * Edge specific attribute statement
 */
class DotEdgeAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    var arrowhead: DotArrowType? by attr()

    var arrowsize: Double? by attr()

    var arrowtail: DotArrowType? by attr()

    var clazz: String? by attr("class")

    var color: String? by attr()

    var colorscheme: String? by attr()

    var comment: String? by attr()

    var constraint: Boolean? by attr()

    var decorate: Boolean? by attr()

    var dir: DotDirType? by attr()

    var edgehref: String? by attr()

    var edgetarget: String? by attr()

    var edgetooltip: String? by attr()

    var edgeurl: String? by attr()

    var fillcolor: String? by attr()

    var fontcolor: String? by attr()

    var fontname: String? by attr()

    var fontsize: Double? by attr()

    var headlp2: Pair<Double, Double>? by attr("head_lp")

    var headlp3: Triple<Double, Double, Double>? by attr("head_lp")

    var headclip: Boolean? by attr()

    var headhref: String? by attr()

    var headlabel: String? by attr()

    var headport: DotPortPos? by attr()

    var headtarget: String? by attr()

    var headtooltip: String? by attr()

    var headurl: String? by attr()

    var href: String? by attr()

    var id: String? by attr()

    var label: String? by attr()

    var labelangle: Double? by attr()

    var labeldistance: Double? by attr()

    var labelfloat: Boolean? by attr()

    var labelfontcolor: String? by attr()

    var labelfontname: String? by attr()

    var labelfontsize: Double? by attr()

    var labelhref: String? by attr()

    var labeltarget: String? by attr()

    var labeltooltip: String? by attr()

    var labelURL: String? by attr()

    var layer: String? by attr()

    var len: Double? by attr()

    var lhead: String? by attr()

    var lp2: Pair<Double, Double>? by attr("lp")

    var lp3: Triple<Double, Double, Double>? by attr("lp")

    var ltail: String? by attr()

    var minlen: Int? by attr()

    var nojustify: Boolean? by attr()

    var penwidth: Double? by attr()

    var pos2: Pair<Double, Double>? by attr("pos")

    var pos3: Triple<Double, Double, Double>? by attr("pos")

    var samehead: String? by attr()

    var sametail: String? by attr()

    var showboxes: Int? by attr()

    var style: DotEdgeStyle? by attr()

    var taillp2: Pair<Double, Double>? by attr("tail_lp")

    var taillp3: Triple<Double, Double, Double>? by attr("tail_lp")

    var tailclip: Boolean? by attr()

    var tailhref: String? by attr()

    var taillabel: String? by attr()

    var tailport: DotPortPos? by attr()

    var tailtarget: String? by attr()

    var tailtooltip: String? by attr()

    var tailURL: String? by attr()

    var target: String? by attr()

    var tooltip: String? by attr()

    var url: String? by attr("URL")

    var weight: Double? by attr()

    var xlabel: String? by attr()

    var xlp2: Pair<Double, Double>? by attr("xlp")

    var xlp3: Triple<Double, Double, Double>? by attr("xlp")
}

enum class DotArrowType {
    NORMAL,
    DOT,
    ODOT,
    NONE,
    EMPTY,
    DIAMOND,
    EDIAMOND,
    BOX,
    OPEN,
    VEE,
    INV,
    INVDOT,
    INVODOT,
    TEE,
    INVEMPTY,
    ODIAMOND,
    CROW,
    OBOX,
    HALFOPEN;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotDirType {
    FORWARD,
    NONE;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotPortPos {
    N,
    NE,
    E,
    SE,
    S,
    SW,
    W,
    NW,
    C,
    DEF;

    override fun toString(): String = when (this) {
        DEF -> "_"
        else -> super.toString().toLowerCase()
    }
}

enum class DotEdgeStyle {
    DASHED,
    DOTTED,
    SOLID,
    INVIS,
    BOLD,
    TAPERED;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotNodeStyle {
    DASHED,
    DOTTED,
    SOLID,
    INVIS,
    BOLD,
    FILLED,
    STRIPED,
    WEDGED,
    DIAGONALS,
    ROUNDED;

    override fun toString(): String = super.toString().toLowerCase()
}

class DotRect(
    val llx: Double,
    val lly: Double,
    val urx: Double,
    val ury: Double
) {
    override fun toString(): String = "$llx,$lly,$urx,$ury"
}

enum class DotClusterMode {
    LOCAL,
    GLOBAL,
    NONE;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotOutputMode {
    BREADTHFIRST,
    NODESFIRST,
    EDGESFIRST;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotPageDir {
    BL,
    BR,
    TL,
    TR,
    RB,
    RT,
    LB,
    LT
}

enum class DotQuadType {
    NORMAL,
    FAST,
    NONE;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotRankDir {
    TB,
    LR,
    BT,
    RL
}

enum class DotSmoothType {
    NONE,
    AVG_DIST,
    GRAPH_DIST,
    POWER_DIST,
    RNG,
    SPRING,
    TRIANGLE;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotNodeShape {
    BOX,
    POLYGON,
    ELLIPSE,
    OVAL,
    CIRCLE,
    POINT,
    EGG,
    TRIANGLE,
    PLAINTEXT,
    PLAIN,
    DIAMOND,
    TRAPEZIUM,
    PARALLELOGRAM,
    HOUSE,
    PENTAGON,
    HEXAGON,
    SEPTAGON,
    OCTAGON,
    DOUBLECIRCLE,
    DOUBLEOCTAGON,
    TRIPLEOCTAGON,
    INVTRIANGLE,
    INVTRAPEZIUM,
    INVHOUSE,
    MDIAMOND,
    MSQUARE,
    MCIRCLE,
    RECT,
    RECTANGLE,
    SQUARE,
    STAR,
    NONE,
    UNDERLINE,
    CYCLINDER,
    NOTE,
    TAB,
    FOLDER,
    BOX3D,
    COMPONENT,
    PROMOTER,
    CDS,
    TERMINATOR,
    UTR,
    PRIMERSITE,
    RESTRICTIONSITE,
    FIVEPOVERHANG,
    THREEPOVERHANG,
    NOVERHANG,
    ASSEMBLY,
    SIGNATURE,
    INSULATOR,
    RIBOSITE,
    RNASTAB,
    PROTEASESITE,
    PROTEINSTAB,
    RPROMOTER,
    RARROW,
    LARROW,
    LPROMOTER,
    RECORD,
    MRECORD;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotSubgraphStyle {
    FILLED,
    STRIPED,
    ROUNDED;

    override fun toString(): String = super.toString().toLowerCase()
}
