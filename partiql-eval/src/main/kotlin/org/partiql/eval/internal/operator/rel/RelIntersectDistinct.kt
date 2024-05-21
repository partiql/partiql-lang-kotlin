package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.RecordUtility.toPartiQLValueList
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class RelIntersectDistinct(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

    // TODO: Add support for equals/hashcode in PQLValue
    @OptIn(PartiQLValueExperimental::class)
    private val seen: MutableSet<List<PartiQLValue>> = mutableSetOf()
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
            if (seen.remove(partiqlRow)) {
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

    @OptIn(PartiQLValueExperimental::class)
    /**
     * Read the entire left-hand-side into our search structure.
     */
    private fun seed() {
        init = true
        for (row in lhs) {
            val partiqlRow = row.toPartiQLValueList()
            seen.add(partiqlRow)
        }
    }
}
