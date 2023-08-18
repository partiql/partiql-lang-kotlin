/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.planner.impl

import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.planner.PlannerEvent
import org.partiql.planner.PlannerEventCallback
import org.partiql.planner.impl.VariableIdAllocator.Companion.VARIABLE_ID_META_TAG
import org.partiql.types.AnyOfType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.StaticType
import java.time.Duration
import java.time.Instant

/** Constructs the column name based on the zero-based index of that column. */
internal fun syntheticColumnName(col: Int): String = "_${col + 1}"

/**
 * Allocates register indexes for all local variables in the plan.
 *
 * Returns pair containing a logical plan where all `var_decl`s have a [VARIABLE_ID_META_TAG] meta indicating the
 * variable index (which can be utilized later when establishing variable scoping) and list of all local variables
 * declared within the plan, which becomes the `locals` sub-node of the `plan` node.
 */
internal fun PartiqlLogical.Plan.allocateVariableIds(): Pair<PartiqlLogical.Plan, List<PartiqlLogicalResolved.LocalVariable>> {
    val allLocals = mutableListOf<PartiqlLogicalResolved.LocalVariable>()
    val planWithAllocatedVariables = VariableIdAllocator(allLocals).transformPlan(this)
    return planWithAllocatedVariables to allLocals.toList()
}

internal val PartiqlLogical.VarDecl.indexMeta
    get() = this.metas[VARIABLE_ID_META_TAG] as? Int ?: error("Meta $VARIABLE_ID_META_TAG was not present")

/**
 * Allocates a unique index to every `var_decl` in the logical plan.  We use metas for this step to avoid a having
 * create another permuted domain.
 */
private class VariableIdAllocator(
    val allLocals: MutableList<PartiqlLogicalResolved.LocalVariable>
) : PartiqlLogical.VisitorTransform() {
    companion object {

        const val VARIABLE_ID_META_TAG = "\$variable_id"
    }
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

/**
 * Returns a human-readable string of [argTypes]. Additionally, for each [AnyOfType], [NullType] and [MissingType] will
 * be filtered.
 */
internal fun stringWithoutNullMissing(argTypes: List<StaticType>): String =
    argTypes.joinToString { it.filterNullMissing().toString() }

/**
 * For [this] [StaticType], filters out [NullType] and [MissingType] from [AnyOfType]s. Otherwise, returns [this].
 */
internal fun StaticType.filterNullMissing(): StaticType =
    when (this) {
        is AnyOfType -> AnyOfType(this.types.filter { !it.isNullOrMissing() }.toSet()).flatten()
        else -> this
    }

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)

/** Convenience function for optionally invoking [PlannerEventCallback] functions. */
internal inline fun <T : Any> PlannerEventCallback?.doEvent(eventName: String, input: Any, crossinline block: () -> T): T {
    if (this == null) return block()
    val startTime = java.time.Instant.now()
    return block().also { output ->
        val endTime = java.time.Instant.now()
        this(PlannerEvent(eventName, input, output, java.time.Duration.between(startTime, endTime)))
    }
}
