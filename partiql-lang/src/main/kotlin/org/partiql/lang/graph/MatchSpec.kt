package org.partiql.lang.graph

// Definitions in this file can be seen as versions of GPML patterns that are more convenient to use
// for the computations in [GraphEngine].
// They may either turn out redundant or prove to be beginnings of something like "plans" for graph queries.
// This is yet to be thought out.

/** Specification of an edge direction.
 *  The boolean fields at the enum values capture their semantics in a way that is useful for
 *  "compiling" them to graph scans.
 */
enum class DirSpec(val wantLeft: Boolean, val wantUndir: Boolean, val wantRight: Boolean) {
    // Attempting to be close to the GPML notation, for mnemonics.
    // However characters <, > are not supported in identifiers, so it is `--)` instead of `->`, etc.
    // The mnemonics attempt might be futile: currently these are only referred to
    // in EvaluatingCompiler.GpmlTranslator.translateDirection, but IntelliJ can't jump to that usage from here!
    `(--`(true, false, false),
    `~~~`(false, true, false),
    `--)`(false, false, true),
    `(~~`(true, true, false),
    `~~)`(false, true, true),
    `(-)`(true, false, true),
    `---`(true, true, true), // You'd think this one be denoted `(~)` / `<~>`
}

/** A step in a graph is a triple of adjacent node, edge, node.
 *  A StepSpec describes a set of steps of interest.  */
data class StepSpec(val dirSpec: DirSpec, val tripleSpec: Triple<LabelSpec, LabelSpec, LabelSpec>)

/** A variable in a graph pattern binding a node or an edge. */
typealias Variable = String

sealed class ElemSpec {
    abstract val binder: Variable?
}
data class NodeSpec(override val binder: Variable?, val label: LabelSpec) : ElemSpec()
data class EdgeSpec(override val binder: Variable?, val label: LabelSpec, val dir: DirSpec) : ElemSpec()

/** A stride is a sequence like
 *           node, edge, node, edge, ..., node
 *  that is, strictly alternating nodes and edges, starting and ending with a node.
 *  It is used as an intermediate step in computing path matches.
 */
data class Stride(val elems: List<Graph.Elem>)

/** Translation of a path pattern into a "plan" for [GraphEngine].
 */
data class StrideSpec(val elems: List<ElemSpec>)

/** The result of matching a [StrideSpec] in a graph.
 *  Each [Stride] in [result] is one valid match for [spec].
 *  Keeping [spec] within the result is for maintaining the association between
 *  [Variable]s and graph elements in [result] that the variables matched. */
data class StrideResult(val spec: StrideSpec, val result: List<Stride>)

/** Translation of a graph match pattern -- a collection of path patterns --
 *  into a "plan" for [GraphEngine]. */
data class MatchSpec(val strides: List<StrideSpec>) {
    init { check(strides.size > 0) }
}

/** The result of matching [specs], a list of stride specs derived from a list of path patterns.
 *  The result is a "table" where each "column" is headed by a stride spec from [specs] and contains matching strides.
 *  That is, each row contains strides matching each stride spec.
 */
data class MatchResult(val specs: List<StrideSpec>, val result: List<List<Stride>>)

/** A [StrideTree] is a plan for computing matches for a stride.
 *  Joins needed to compute a stride can be performed in different orders;
 *  a [StrideTree] represents a chosen order. */
sealed class StrideTree
data class StrideJoin(val left: StrideTree, val right: StrideTree) : StrideTree()
data class StrideLeaf(val stride: StrideSpec) : StrideTree()
