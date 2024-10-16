package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import java.util.TreeMap

internal class RelOpExceptAll(
    lhs: Operator.Relation,
    rhs: Operator.Relation,
) : RelOpPeeking() {

    private val lhs: Operator.Relation = lhs
    private val rhs: Operator.Relation = rhs
    private val seen = TreeMap<Array<Datum>, Int>(DatumArrayComparator)
    private var init: Boolean = false

    override fun openPeeking() {
        lhs.open()
        rhs.open()
        init = false
        seen.clear()
    }

    override fun peek(): Row? {
        if (!init) {
            seed()
        }
        for (row in lhs) {
            row.values.coerceMissing()
            val remaining = seen[row.values] ?: 0
            if (remaining > 0) {
                seen[row.values] = remaining - 1
                continue
            }
            return Row(row.values)
        }
        return null
    }

    override fun closePeeking() {
        lhs.close()
        rhs.close()
        seen.clear()
    }

    /**
     * Read the entire right-hand-side into our search structure.
     */
    private fun seed() {
        init = true
        for (row in rhs) {
            row.values.coerceMissing()
            val n = seen[row.values] ?: 0
            seen[row.values] = n + 1
        }
    }
}
