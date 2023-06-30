package org.partiql.lang.graph

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.graph.GpmlTranslator.normalizeElemList

/** Translate an AST graph pattern into a "plan spec" to be executed by the graph engine.
 *  Currently, the only non-trivial aspect is making sure (in [normalizeElemList]) that node and edge elements alternate.
 *  This (as well as the plan specs) is expected to become more sophisticated
 *  as more graph pattern features are supported (esp. quantifiers and alternation).
 */
object GpmlTranslator {

    /** The main entry point into the translator. */
    fun translateGpmlPattern(gpml: PartiqlAst.GpmlPattern): MatchSpec {

        if (gpml.selector != null) TODO("Evaluation of GPML selectors is not yet supported")
        return MatchSpec(gpml.patterns.map { StrideSpec(normalizeElemList(translatePathPat(it))) })
    }

    fun translatePathPat(path: PartiqlAst.GraphMatchPattern): List<ElemSpec> {
        if (path.prefilter != null || path.quantifier != null || path.restrictor != null || path.variable != null)
            TODO("Not yet supported in evaluating a GPML path pattern: prefilters, quantifiers, restrictors, binder variables.")
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

    fun translateLabels(labelSpec: PartiqlAst.GraphLabelSpec?): LabelSpec {
        return when (labelSpec) {
            null -> LabelSpec.Wildcard
            is PartiqlAst.GraphLabelSpec.GraphLabelName -> LabelSpec.Name(labelSpec.name.text)
            is PartiqlAst.GraphLabelSpec.GraphLabelWildcard -> LabelSpec.Wildcard
            else -> TODO("Not yet supported graph label pattern: $labelSpec")
        }
    }

    fun translateDirection(dir: PartiqlAst.GraphMatchDirection): DirSpec =
        when (dir) {
            is PartiqlAst.GraphMatchDirection.EdgeLeft -> DirSpec.DirL__
            is PartiqlAst.GraphMatchDirection.EdgeUndirected -> DirSpec.Dir_U_
            is PartiqlAst.GraphMatchDirection.EdgeRight -> DirSpec.Dir__R
            is PartiqlAst.GraphMatchDirection.EdgeLeftOrUndirected -> DirSpec.DirLU_
            is PartiqlAst.GraphMatchDirection.EdgeUndirectedOrRight -> DirSpec.Dir_UR
            is PartiqlAst.GraphMatchDirection.EdgeLeftOrRight -> DirSpec.DirL_R
            is PartiqlAst.GraphMatchDirection.EdgeLeftOrUndirectedOrRight -> DirSpec.DirLUR
        }

    /** Make sure there is proper alternation of NodeSpec and EdgeSpec entries,
     *  by inserting a [NodeSpec] between adjacent [EdgeSpec]s.
     *  TODO: Deal with adjacent [NodeSpec]s -- by "unification" or prohibit.
     */
    fun normalizeElemList(elems: List<ElemSpec>): List<ElemSpec> {
        val fillerNode = NodeSpec(null, LabelSpec.Wildcard)
        val normalized = mutableListOf<ElemSpec>()
        var expectNode = true
        for (x in elems) {
            if (expectNode) {
                when (x) {
                    is NodeSpec -> { normalized.add(x); expectNode = false }
                    is EdgeSpec -> { normalized.add(fillerNode); normalized.add(x) }
                }
            } else { // expectNode == false
                when (x) {
                    is NodeSpec -> TODO("Deal with adjacent nodes in a pattern.  Unify? Prohibit?")
                    is EdgeSpec -> { normalized.add(x); expectNode = true }
                }
            }
        }
        if (expectNode) normalized.add(fillerNode)
        return normalized.toList()
    }
}
