package org.partiql.lang.planner.transforms

import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved

/**
 * Allocates register indexes for all local variables in the plan.
 *
 * Returns pair containing a logical plan where all `var_decl`s have a [VARIABLE_ID_META_TAG] meta indicating the
 * variable index (which can be utilized later when establishing variable scoping) and list of all local variables
 * declared within the plan, which becomes the `locals` sub-node of the `plan` node.
 */
internal fun PartiqlLogical.Plan.allocateVariableIds(): Pair<PartiqlLogical.Plan, List<PartiqlLogicalResolved.LocalVariable>> {

    var allLocals = mutableListOf<PartiqlLogicalResolved.LocalVariable>()
    val planWithAllocatedVariables = VariableIdAllocator(allLocals).transformPlan(this)
    return planWithAllocatedVariables to allLocals.toList()
}

private const val VARIABLE_ID_META_TAG = "\$variable_id"

internal val PartiqlLogical.VarDecl.indexMeta
    get() = this.metas[VARIABLE_ID_META_TAG] as? Int ?: error("Meta $VARIABLE_ID_META_TAG was not present")

/**
 * Allocates a unique index to every `var_decl` in the logical plan.  We use metas for this step to avoid a having
 * create another permuted domain.
 */
private class VariableIdAllocator(
    val allLocals: MutableList<PartiqlLogicalResolved.LocalVariable>
) : PartiqlLogical.VisitorTransform() {
    private var nextVariableId = 0

    override fun transformVarDecl(node: PartiqlLogical.VarDecl): PartiqlLogical.VarDecl =
        node.withMeta(VARIABLE_ID_META_TAG, nextVariableId).also {

            allLocals.add(
                PartiqlLogicalResolved.build {
                    localVariable(node.name.text, nextVariableId.toLong())
                }
            )

            nextVariableId++
        }
}
