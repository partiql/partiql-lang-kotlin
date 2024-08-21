package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.RecordUtility.toDatumArrayCoerceMissing
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import java.util.TreeMap

internal class RelIntersectAll(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

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
            val partiqlRow = row.toDatumArrayCoerceMissing()
            val remaining = seen[partiqlRow] ?: 0
            if (remaining > 0) {
                seen[partiqlRow] = remaining - 1
                return Record.of(*partiqlRow)
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
            val partiqlRow = row.toDatumArrayCoerceMissing()
            val alreadySeen = seen[partiqlRow] ?: 0
            seen[partiqlRow] = alreadySeen + 1
        }
    }
}
