package org.partiql.lang.eval.internal.exclude

import org.partiql.lang.domains.PartiqlPhysical

/**
 * Represents all the compiled `EXCLUDE` paths that start with the same [CompiledExcludeExpr.root]. This variant of
 * [ExcludeNode] represents the top-level root node of the exclude tree.
 *
 * Notably, redundant paths (i.e. exclude paths that exclude values already excluded by other paths) will be removed.
 */
internal data class CompiledExcludeExpr(
    val root: Int,
    override val leaves: MutableSet<ExcludeLeaf>,
    override val branches: MutableSet<ExcludeBranch>
) : ExcludeNode(leaves, branches) {
    companion object {
        fun empty(root: Int): CompiledExcludeExpr {
            return CompiledExcludeExpr(root, mutableSetOf(), mutableSetOf())
        }
    }
}

/**
 * Represent all the `EXCLUDE` paths that start with the same [ExcludeBranch.step] that also have additional steps
 * (i.e. final step is at a deeper level). This variant of [ExcludeNode] represents inner nodes (i.e. non-top-level)
 * nodes of the exclude tree.
 */
internal data class ExcludeBranch(
    val step: PartiqlPhysical.ExcludeStep,
    override val leaves: MutableSet<ExcludeLeaf>,
    override val branches: MutableSet<ExcludeBranch>
) : ExcludeNode(leaves, branches) {
    companion object {
        fun empty(step: PartiqlPhysical.ExcludeStep): ExcludeBranch {
            return ExcludeBranch(step, mutableSetOf(), mutableSetOf())
        }
    }
}

/**
 * Represents all the `EXCLUDE` paths that have a final exclude step at the current level. This variant of [ExcludeNode]
 * represents the leaves in our exclude tree.
 */
internal data class ExcludeLeaf(
    val step: PartiqlPhysical.ExcludeStep,
) : ExcludeNode(mutableSetOf(), mutableSetOf())

/**
 * A tree representation of the exclude paths that will eliminate redundant paths (i.e. exclude paths that exclude
 * values already excluded by other paths).
 *
 * The idea behind this tree representation is that at a current level (i.e. path step index), we keep track of the
 * - Exclude paths that have a final exclude step at the current level. This set of tuple attributes and collection
 * indexes to remove at the current level is modeled as a set of leaves (i.e. [ExcludeLeaf]).
 * - Exclude paths that have additional steps (their final step is at a deeper level). This is modeled as a set of
 * branches [ExcludeBranch] to group all exclude paths that share the same current step.
 *
 * For example, let's say we have exclude paths
 *       a.b,    -- assuming root resolves to 0
 *       x.y.z1, -- assuming root resolves to 1
 *       x.y.z2  -- assuming root resolves to 1
 *       ^ ^ ^
 * Level 1 2 3
 *
 * These exclude paths would be converted to the following [CompiledExcludeExpr]s in [ExcludeNode]s:
 * ```
 * // For demonstration purposes, the syntax '<string>' corresponds to the exclude tuple attribute step of <string>
 * CompiledExcludeExpr(                       // Root 0 (i.e. 'a')
 *     root = 0,
 *     leaves = mutableSetOf(
 *         ExcludeLeaf(step = 'b')            // Exclude 'b' at level 2
 *     ),
 *     branches = mutableSetOf()              // No further exclusions
 * ),
 * CompiledExcludeExpr(                       // Root 1 (i.e. 'x')
 *     root = 1,
 *     leaves = mutableSetOf(),               // No exclusions at level 2
 *     branches = mutableSetOf(
 *         ExcludeBranch(
 *             step = 'y',
 *             leaves = mutableSetOf(
 *                 ExcludeLeaf(step = 'z1'),  // Exclude 'z1` at level 3
 *                 ExcludeLeaf(step = 'z2')   // Exclude `z2` at level 3
 *             )
 *             branches = mutableSetOf()      // No further exclusions
 *         )
 *     )
 * )
 */
internal sealed class ExcludeNode(
    open val leaves: MutableSet<ExcludeLeaf>,
    open val branches: MutableSet<ExcludeBranch>
) {
    private fun addLeaf(step: PartiqlPhysical.ExcludeStep) {
        when (step) {
            is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr -> {
                if (leaves.contains(ExcludeLeaf(PartiqlPhysical.build { excludeTupleWildcard() }))) {
                    // leaves contain wildcard; do not add; e.g. a.* and a.b -> keep a.*
                } else {
                    // add to leaves
                    leaves.add(ExcludeLeaf(step))
                    // remove from branches; e.g. a.b.c and a.b -> keep a.b
                    branches.removeIf { subBranch ->
                        step == subBranch.step
                    }
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard -> {
                leaves.add(ExcludeLeaf(step))
                // remove all tuple attribute exclude steps from leaves
                leaves.removeIf { subLeaf ->
                    subLeaf.step is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr
                }
                // remove all tuple attribute/wildcard exclude steps from branches
                branches.removeIf { subBranch ->
                    subBranch.step is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr || subBranch.step is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex -> {
                if (leaves.contains(ExcludeLeaf(PartiqlPhysical.build { excludeCollectionWildcard() }))) {
                    // leaves contains wildcard; do not add; e.g a[*] and a[1] -> keep a[*]
                } else {
                    // add to leaves
                    leaves.add(ExcludeLeaf(step))
                    // remove from branches; e.g. a.b[2].c and a.b[2] -> keep a.b[2]
                    branches.removeIf { subBranch ->
                        step == subBranch.step
                    }
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard -> {
                leaves.add(ExcludeLeaf(step))
                // remove all collection index exclude steps from leaves
                leaves.removeIf { subLeaf ->
                    subLeaf.step is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex
                }
                // remove all collection index/wildcard exclude steps from branches
                branches.removeIf { subBranch ->
                    subBranch.step is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex || subBranch.step is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard
                }
            }
        }
    }

    private fun addBranch(steps: List<PartiqlPhysical.ExcludeStep>) {
        val head = steps.first()
        val tail = steps.drop(1)
        when (head) {
            is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr -> {
                if (leaves.contains(ExcludeLeaf(PartiqlPhysical.build { excludeTupleWildcard() })) || leaves.contains(
                        ExcludeLeaf(head)
                    )
                ) {
                    // leaves contains tuple wildcard or attr; do not add to branches
                    // e.g. a.* and a.b.c -> a.*
                } else {
                    val existingBranch = branches.find { subBranch ->
                        head == subBranch.step
                    } ?: ExcludeBranch.empty(head)
                    branches.remove(existingBranch)
                    existingBranch.addNode(tail)
                    branches.add(existingBranch)
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard -> {
                if (leaves.any { it.step is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard }) {
                    // tuple wildcard in leaves; do nothing
                } else {
                    val existingBranch = branches.find { subBranch ->
                        head == subBranch.step
                    } ?: ExcludeBranch.empty(head)
                    branches.remove(existingBranch)
                    existingBranch.addNode(tail)
                    branches.add(existingBranch)
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex -> {
                if (leaves.contains(ExcludeLeaf(PartiqlPhysical.build { excludeCollectionWildcard() })) || leaves.contains(
                        ExcludeLeaf(head)
                    )
                ) {
                    // leaves contains collection wildcard or index; do not add to branches
                    // e.g. a[*] and a[*][1] -> a[*]
                } else {
                    val existingBranch = branches.find { subBranch ->
                        head == subBranch.step
                    } ?: ExcludeBranch.empty(head)
                    branches.remove(existingBranch)
                    existingBranch.addNode(tail)
                    branches.add(existingBranch)
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard -> {
                if (leaves.any { it.step is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard }) {
                    // collection wildcard in leaves; do nothing
                } else {
                    val existingBranch = branches.find { subBranch ->
                        head == subBranch.step
                    } ?: ExcludeBranch.empty(head)
                    branches.remove(existingBranch)
                    existingBranch.addNode(tail)
                    branches.add(existingBranch)
                }
            }
        }
    }

    internal fun addNode(steps: List<PartiqlPhysical.ExcludeStep>) {
        when (steps.size) {
            1 -> this.addLeaf(steps.first())
            else -> this.addBranch(steps)
        }
    }
}

/**
 * Creates a list of [CompiledExcludeExpr] with each index of the resulting list corresponding to a different
 * exclude path root.
 */
internal fun compileExcludeClause(excludeClause: PartiqlPhysical.Bexpr.ExcludeClause): List<CompiledExcludeExpr> {
    val excludeExprs = excludeClause.exprs
    val compiledExcludeExprs = excludeExprs
        .groupBy { it.root }
        .map { (root, exclusions) ->
            exclusions.fold(CompiledExcludeExpr.empty(root.value.toInt())) { acc, exclusion ->
                acc.addNode(exclusion.steps)
                acc
            }
        }
    return compiledExcludeExprs
}
