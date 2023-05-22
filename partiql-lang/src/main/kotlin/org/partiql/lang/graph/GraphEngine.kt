package org.partiql.lang.graph

import org.partiql.lang.util.tail

object GraphEngine {

    /** Compute any [StepSpec] (an elementary node-edge-node path, in any direction),
     *  by combining the scan methods from [Graph].
     *  TODO: Consider making this method as *the* API of Graph,
     *  as it is, by itself, tirned out to be sufficient, so far as a "window" in a graph
     *  for the needs of this engine's implementation.
     */
    fun Graph.getMatchingSteps(spec: StepSpec): List<Triple<Graph.Node, Graph.Edge, Graph.Node>> {
        val (dirSpec, tripleSpec) = spec
        val result =
            (if (dirSpec.wantUndir) this.scanUndir(tripleSpec) else emptyList()) +
                (if (dirSpec.wantLeft && dirSpec.wantRight) this.scanDirectedBlunt(tripleSpec) else emptyList()) +
                (if (!dirSpec.wantLeft && dirSpec.wantRight) this.scanDirectedStraight(tripleSpec) else emptyList()) +
                (if (dirSpec.wantLeft && !dirSpec.wantRight) this.scanDirectedFlipped(tripleSpec) else emptyList())
        return result
    }

    /** The entry point for computing a graph pattern (translated to the [MatchSpec] "plan")
     *  against a graph. */
    fun evaluate(graph: Graph, matchSpec: MatchSpec): MatchResult {
        val strideResults = matchSpec.strides.map { evaluateStride(graph, it) }
        return joinStridesOnBinders(strideResults)
    }

    private fun evaluateStride(graph: Graph, stride: StrideSpec): StrideResult {
        if (stride.elems.size == 1) return evaluateNodeStride(graph, stride)

        val plan = planStride(stride)
        check(stride == restoreStrideSpec(plan)) {
            "Bad stride plan, not equivalent to the original stride."
        }
        return evaluatePlan(graph, plan)
    }

    // This is for a node-only pattern like  (g MATCH (x)). TODO: Something less ugly / more unified with the rest?
    private fun evaluateNodeStride(graph: Graph, stride: StrideSpec): StrideResult {
        check(stride.elems.size == 1)
        return when (val node = stride.elems[0]) {
            is EdgeSpec -> error("Bug: evaluateNodeStride should not be called on an EdgeSpec")
            is NodeSpec -> StrideResult(
                stride,
                graph.scanNodes(node.label).map { Stride(listOf(it)) }
            )
        }
    }

    /** Determine the order of joining the steps in a stride.
     *  Assumes that [stride] contains a properly alternating list: n, e, n, ..., e, n.
     *
     *  At the moment, this just takes one straightforward order.
     *  Conceivably, this can become something more sophisticated.
     *  Would it be more appropriate to do this planing during GPML translation
     *  (in case there is more information available there for guiding the planning?)
     */
    private fun planStride(stride: StrideSpec): StrideTree {
        check(stride.elems.size >= 3)

        fun leafFrom3(elems: List<ElemSpec>): StrideLeaf {
            check(elems[0] is NodeSpec)
            check(elems[1] is EdgeSpec)
            check(elems[2] is NodeSpec)
            return StrideLeaf(StrideSpec(elems.take(3)))
        }

        fun planRightLeaning(elems: List<ElemSpec>): StrideTree =
            when (elems.size) {
                0, 1, 2 -> error("Bug: planRightLeaning should not be called on a spec shorter than 3 ")
                3 -> leafFrom3(elems)
                else -> StrideJoin( // Note: 2nd node (3rd element) participates in both sides of the join
                    leafFrom3(elems),
                    planRightLeaning(elems.drop(2))
                )
            }

        return planRightLeaning(stride.elems)
    }

    private fun restoreStrideSpec(strideTree: StrideTree): StrideSpec {
        fun restore(tree: StrideTree): List<ElemSpec> =
            when (tree) {
                is StrideLeaf -> tree.stride.elems
                is StrideJoin -> {
                    val left = restore(tree.left)
                    val right = restore(tree.right)
                    check(left.last() is NodeSpec)
                    check(right.first() is NodeSpec)
                    check(left.last() == right.first())
                    left + right.tail
                }
            }
        return StrideSpec(restore(strideTree))
    }

    /** Perform graph scans and joins, as the [StrideTree] plan specifies. */
    private fun evaluatePlan(graph: Graph, plan: StrideTree): StrideResult {
        return when (plan) {
            is StrideLeaf -> {
                val step = plan.stride.elems
                check(step.size == 3, { "A leaf stride in a StrideTree plan must have exactly 3 elements" })
                val lft = step[0] as NodeSpec
                val edg = step[1] as EdgeSpec
                val rgt = step[2] as NodeSpec
                val stepSpec = StepSpec(edg.dir, Triple(lft.label, edg.label, rgt.label))
                val triples = graph.getMatchingSteps(stepSpec)
                // if the same variable is used in the step's [NodeSpec]s, it should bind to the same node:
                val bindCheck: (Triple<Graph.Node, Graph.Edge, Graph.Node>) -> Boolean =
                    if (lft.binder != null && rgt.binder != null && lft.binder == rgt.binder) {
                        triple ->
                        triple.first == triple.third
                    } else { triple -> true }
                val prunedTriples = triples.filter { bindCheck(it) }
                StrideResult(
                    plan.stride,
                    prunedTriples.map { Stride(listOf(it.first, it.second, it.third)) }
                )
            }

            is StrideJoin -> {
                joinAdjacentStrides(
                    evaluatePlan(graph, plan.left),
                    evaluatePlan(graph, plan.right)
                )
            }
        }
    }

    fun joinAdjacentStrides(left: StrideResult, right: StrideResult): StrideResult {
        val leftSpec = left.spec.elems
        val rightSpec = right.spec.elems
        check(leftSpec.last() == rightSpec.first())
        check(rightSpec.first() is NodeSpec)
        val joinedSpec = leftSpec + rightSpec.tail
        val joinCondition = stridesJoinable(left.spec, right.spec)

        val joined = mutableListOf<Stride>()
        for (lft in left.result) {
            for (rgt in right.result) {
                if (joinCondition(lft, rgt)) {
                    joined.add(Stride(lft.elems + rgt.elems.tail))
                }
            }
        }
        return StrideResult(
            StrideSpec(joinedSpec),
            joined.toList()
        )
    }

    /** Given two [StrideSpec]s for adjacent strides,
     * formulate a predicate for checking whether two adjacent strides are joinable.
     * They are joinable if
     *  - The last element of the left stride is the same as the first element of the right one.
     *  - For each common binding variable, the two strides hold the same element.
     *  This assumes that each of the two strides has been properly joined before,
     *  in that if a variable occurs in the stride multiple times, it already binds to the same element in the stride.
     */
    fun stridesJoinable(leftSpec: StrideSpec, rightSpec: StrideSpec): (Stride, Stride) -> Boolean {
        // Find variables that are common between the left and right stride specs and record their indexes.
        // Note: even though a variable x can be repeated multiple times within a stride spec,
        // we only need to note its first occurrence in each -- because of the above assumption.
        val joinVars = mutableMapOf<Variable, Pair<Int, Int>>()
        for ((lftIdx, lftEltSpec) in leftSpec.elems.withIndex()) {
            lftEltSpec.binder?.let { lftVar ->
                if (! joinVars.keys.contains(lftVar)) {
                    val rgtIdx = rightSpec.elems.indexOfFirst { rgtElem -> rgtElem.binder?.let { it == lftVar } ?: false }
                    if (rgtIdx != -1)
                        joinVars[lftVar] = lftIdx to rgtIdx
                }
            }
        }
        // The index pairs are points where strides being joined must have the same elements.
        val joinPoints: List<Pair<Int, Int>> = joinVars.values +
            Pair(leftSpec.elems.lastIndex, 0) // always join on last left and first right - adjacency

        // Now can formulate an index-based join condition on strides:
        return { leftStride: Stride, rightStride: Stride ->
            /** Relies on [Graph.Elem]s having proper, pointer-based, equality */
            joinPoints.all { (lft, rgt) -> leftStride.elems[lft] == rightStride.elems[rgt] }
        }
    }

    /** Joins results of stride matches on distinct path patterns of a graph pattern.
     *  In general, this is a cartesian product, but it is whittled down by the requirement
     *  that a given binder variable binds to the same graph element in each individual answer.
     */
    fun joinStridesOnBinders(strides: List<StrideResult>): MatchResult {
        return when (strides.size) {
            0 -> { error("Bug: should not call joinStridesOnBinders on a zero-length list of stride results.") }
            1 -> {
                val (spec, res) = strides[0]
                MatchResult(
                    listOf(spec),
                    res.map { listOf(it) }
                )
            }
            else -> TODO("Later: non-trivial join of strides on binders (when there is two or more strides).")
        }
    }
}
