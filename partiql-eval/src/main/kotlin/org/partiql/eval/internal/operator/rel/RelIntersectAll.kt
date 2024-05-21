package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.RecordUtility.toPartiQLValueList
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class RelIntersectAll(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

    // TODO: Add support for equals/hashcode in PQLValue
    @OptIn(PartiQLValueExperimental::class)
    private val seen: MutableMap<List<PartiQLValue>, Int> = mutableMapOf()
    private var init: Boolean = false

    @OptIn(PartiQLValueExperimental::class)
    override fun openPeeking(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        init = false
        seen.clear()
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun peek(): Record? {
        if (!init) {
            seed()
        }
        for (row in rhs) {
            val partiqlRow = row.toPartiQLValueList()
            val remaining = seen[partiqlRow] ?: 0
            if (remaining > 0) {
                seen[partiqlRow] = remaining - 1
                return row
            }
        }
        return null
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun closePeeking() {
        lhs.close()
        rhs.close()
        seen.clear()
    }

    /**
     * Read the entire left-hand-side into our search structure.
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun seed() {
        init = true
        for (row in lhs) {
            val partiqlRow = row.toPartiQLValueList()
            val alreadySeen = seen[partiqlRow] ?: 0
            seen[partiqlRow] = alreadySeen + 1
        }
    }
}
