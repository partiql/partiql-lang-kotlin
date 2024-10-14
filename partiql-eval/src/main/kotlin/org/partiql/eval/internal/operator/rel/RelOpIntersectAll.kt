package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.operator.Record
import org.partiql.spi.value.Datum
import java.util.TreeMap

internal class RelOpIntersectAll(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelOpPeeking() {

    private val seen = TreeMap<Array<Datum>, Int>(DatumArrayComparator)
    private var init: Boolean = false

    override fun openPeeking(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        init = false
        seen.clear()
    }

    override fun peek(): Record? {
        if (!init) {
            seed()
        }
        for (row in rhs) {
            row.values.coerceMissing()
            val remaining = seen[row.values] ?: 0
            if (remaining > 0) {
                seen[row.values] = remaining - 1
                return Record(row.values)
            }
        }
        return null
    }

    override fun closePeeking() {
        lhs.close()
        rhs.close()
        seen.clear()
    }

    /**
     * Read the entire left-hand-side into our search structure.
     */
    private fun seed() {
        init = true
        for (row in lhs) {
            row.values.coerceMissing()
            val alreadySeen = seen[row.values] ?: 0
            seen[row.values] = alreadySeen + 1
        }
    }
}
