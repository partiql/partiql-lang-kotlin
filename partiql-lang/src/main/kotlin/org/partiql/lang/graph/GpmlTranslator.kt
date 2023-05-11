package org.partiql.lang.graph

import org.partiql.lang.domains.PartiqlAst
import org.partiql.pig.runtime.SymbolPrimitive

/** Translate an AST graph pattern into a "plan spec" to be executed by the graph engine.
 *  Currently, the only non-trivial aspect is making sure (in [patchElemList]) that node and edge elements alternate.
 *  This (as well as the plan specs) is expected to become more sophisticated
 *  as more graph pattern features are supported (esp. quantifiers and alternation).
 */
object GpmlTranslator {

    /** The main entry point into the translator. */
    fun translateGpmlPattern(gpml: PartiqlAst.GpmlPattern): MatchSpec {

        if (gpml.selector != null) TODO("Evaluation of GPML selectors is not yet supported")
        return MatchSpec(gpml.patterns.map { StrideSpec(patchElemList(translatePathPat(it))) })
    }

    fun translatePathPat(path: PartiqlAst.GraphMatchPattern): List<ElemSpec> {
        if (path.prefilter != null || path.quantifier != null || path.restrictor != null || path.variable != null)
            TODO("Not yet supported in evaluating a GPML path pattern: prefiletrs, quantifiers, restrictors, binder variables.")
        return path.parts.flatMap { translatePartPat(it) }
    }

    fun translatePartPat(part: PartiqlAst.GraphMatchPatternPart): List<ElemSpec> =
        when (part) {
            is PartiqlAst.GraphMatchPatternPart.Node ->
                listOf(translateNodePat(part))
            is PartiqlAst.GraphMatchPatternPart.Edge ->
                listOf(translateEdgePat(part))
            is PartiqlAst.GraphMatchPatternPart.Pattern ->
                translatePathPat(part.pattern)
        }

    fun translateNodePat(node: PartiqlAst.GraphMatchPatternPart.Node): NodeSpec {
        if (node.prefilter != null) TODO("Not yet supported in evaluating a GPML node pattern: prefilter.")
        return NodeSpec(
            binder = node.variable?.text,
            label = translateLabels(node.label)
        )
    }

    fun translateEdgePat(edge: PartiqlAst.GraphMatchPatternPart.Edge): EdgeSpec {
        if (edge.prefilter != null || edge.quantifier != null)
            TODO("Not yet supported in evaluating a GPML edge pattern: prefilter, quantifier.")
        return EdgeSpec(
            binder = edge.variable?.text,
            label = translateLabels(edge.label),
            dir = translateDirection(edge.direction)
        )
    }

    fun translateLabels(labels: List<SymbolPrimitive>): LabelSpec {
        return when (labels.size) {
            0 -> LabelSpec.Whatever
            1 -> LabelSpec.OneOf(labels[0].text)
            else -> TODO("Not yet supported in evaluating a GPML graph element pattern: multiple/alternating labels")
        }
    }

    fun translateDirection(dir: PartiqlAst.GraphMatchDirection): DirSpec =
        when (dir) {
            is PartiqlAst.GraphMatchDirection.EdgeLeft -> DirSpec.`(--`
            is PartiqlAst.GraphMatchDirection.EdgeUndirected -> DirSpec.`~~~`
            is PartiqlAst.GraphMatchDirection.EdgeRight -> DirSpec.`--)`
            is PartiqlAst.GraphMatchDirection.EdgeLeftOrUndirected -> DirSpec.`(~~`
            is PartiqlAst.GraphMatchDirection.EdgeUndirectedOrRight -> DirSpec.`~~)`
            is PartiqlAst.GraphMatchDirection.EdgeLeftOrRight -> DirSpec.`(-)`
            is PartiqlAst.GraphMatchDirection.EdgeLeftOrUndirectedOrRight -> DirSpec.`---`
        }

    /** Make sure there is proper alternation of NodeSpec and EdgeSpec entries,
     *  by inserting a [NodeSpec] between adjacent [EdgeSpec]s.
     *  TODO: Deal with adjacent [NodeSpec]s -- by "unification" or prohibit.
     */
    fun patchElemList(elems: List<ElemSpec>): List<ElemSpec> {
        // println("Before patching: ${elems}")
        val fillerNode = NodeSpec(null, LabelSpec.Whatever)
        val patched = mutableListOf<ElemSpec>()
        var expectNode = true
        for (x in elems) {
            if (expectNode) {
                when (x) {
                    is NodeSpec -> { patched.add(x); expectNode = false }
                    is EdgeSpec -> { patched.add(fillerNode); patched.add(x) }
                }
            } else { // expectNode == false
                when (x) {
                    is NodeSpec -> TODO("Deal with adjacent nodes in a pattern.  Unify? Prohibit?")
                    is EdgeSpec -> { patched.add(x); expectNode = true }
                }
            }
        }
        if (expectNode) patched.add(fillerNode)
        // println("After  patching: ${patched}")
        return patched.toList()
    }
}
