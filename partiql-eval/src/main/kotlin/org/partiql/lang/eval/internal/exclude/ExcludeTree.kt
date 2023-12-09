package org.partiql.lang.eval.internal.exclude

import org.partiql.lang.domains.PartiqlPhysical

/**
 * TODO Alan: docs + cleanup
 */
internal sealed class ExcludeNode(
    val leaves: MutableSet<Leaf>,
    val branches: MutableSet<Node>
) {
    private fun addLeaf(step: PartiqlPhysical.ExcludeStep) {
        when (step) {
            is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr -> {
                if (leaves.contains(Leaf(PartiqlPhysical.build { excludeTupleWildcard() }))) {
                    // contains wildcard; do not add; e.g. a.* and a.b -> keep a.*
                } else {
                    // add to entries to remove
                    leaves.add(Leaf(step))
                    // remove from other steps; e.g. a.b.c and a.b -> keep a.b
                    branches.removeIf { subBranch ->
                        step == subBranch.step
                    }
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard -> {
                leaves.add(Leaf(step))
                // remove all tuple attribute exclude steps
                leaves.removeIf { subLeaf ->
                    subLeaf.step is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr
                }
                // remove all tuple attribute/wildcard exclude steps from deeper levels
                branches.removeIf { subBranch ->
                    subBranch.step is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr || subBranch.step is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex -> {
                if (leaves.contains(Leaf(PartiqlPhysical.build { excludeCollectionWildcard() }))) {
                    // contains wildcard; do not add; e.g a[*] and a[1] -> keep a[*]
                } else {
                    // add to entries to remove
                    leaves.add(Leaf(step))
                    // remove from other steps; e.g. a.b[2].c and a.b[2] -> keep a.b[2]
                    branches.removeIf { subBranch ->
                        step == subBranch.step
                    }
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard -> {
                leaves.add(Leaf(step))
                // remove all collection index exclude steps
                leaves.removeIf { subLeaf ->
                    subLeaf.step is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex
                }
                // remove all collection index/wildcard exclude steps from deeper levels
                branches.removeIf { subBranch ->
                    subBranch.step is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex || subBranch.step is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard
                }
            }
        }
    }

    private fun addNode(steps: List<PartiqlPhysical.ExcludeStep>) {
        val head = steps.first()
        when (head) {
            is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr -> {
                if (leaves.contains(Leaf(PartiqlPhysical.build { excludeTupleWildcard() })) || leaves.contains(
                        Leaf(head)
                    )
                ) {
                    // remove set contains tuple wildcard or attr; do not add to other steps;
                    // e.g. a.* and a.b.c -> a.*
                } else {
                    val existingBranch = branches.find { subBranch ->
                        head == subBranch.step
                    } ?: Node.empty(head)
                    branches.remove(existingBranch)
                    existingBranch.addSteps(steps.drop(1))
                    branches.add(existingBranch)
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard -> {
                if (leaves.any { it.step is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard }) {
                    // tuple wildcard at current level; do nothing
                } else {
                    val existingBranch = branches.find { subBranch ->
                        head == subBranch.step
                    } ?: Node.empty(head)
                    branches.remove(existingBranch)
                    existingBranch.addSteps(steps.drop(1))
                    branches.add(existingBranch)
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex -> {
                if (leaves.contains(Leaf(PartiqlPhysical.build { excludeCollectionWildcard() })) || leaves.contains(
                        Leaf(head)
                    )
                ) {
                    // remove set contains collection wildcard or index; do not add to other steps;
                    // e.g. a[*] and a[*][1] -> a[*]
                } else {
                    val existingBranch = branches.find { subBranch ->
                        head == subBranch.step
                    } ?: Node.empty(head)
                    branches.remove(existingBranch)
                    existingBranch.addSteps(steps.drop(1))
                    branches.add(existingBranch)
                }
            }
            is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard -> {
                if (leaves.any { it.step is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard }) {
                    // collection wildcard at current level; do nothing
                } else {
                    val existingBranch = branches.find { subBranch ->
                        head == subBranch.step
                    } ?: Node.empty(head)
                    branches.remove(existingBranch)
                    existingBranch.addSteps(steps.drop(1))
                    branches.add(existingBranch)
                }
            }
        }
    }

    fun addSteps(steps: List<PartiqlPhysical.ExcludeStep>) {
        when (steps.size) {
            1 -> this.addLeaf(steps.first())
            else -> this.addNode(steps)
        }
    }
}

/**
 * Represents an instance of a compiled `EXCLUDE` expression. Notably, this expr will have redundant steps removed.
 */
internal class CompiledExcludeExpr(
    val root: Int,
    leaves: MutableSet<Leaf>,
    branches: MutableSet<Node>
) : ExcludeNode(leaves, branches) {
    companion object {
        fun empty(root: Int): CompiledExcludeExpr {
            return CompiledExcludeExpr(root, mutableSetOf(), mutableSetOf())
        }
    }
}

internal class Node(
    val step: PartiqlPhysical.ExcludeStep,
    leaves: MutableSet<Leaf>,
    branches: MutableSet<Node>
) : ExcludeNode(leaves, branches) {
    companion object {
        fun empty(step: PartiqlPhysical.ExcludeStep): Node {
            return Node(step, mutableSetOf(), mutableSetOf())
        }
    }
}

internal class Leaf(
    val step: PartiqlPhysical.ExcludeStep,
) : ExcludeNode(mutableSetOf(), mutableSetOf())
