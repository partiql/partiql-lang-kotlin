package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.RecordUtility.toDatumArrayCoerceMissing
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import java.util.TreeMap

internal class RelExceptAll(
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
        for (row in lhs) {
            val partiqlRow = row.toDatumArrayCoerceMissing()
            val remaining = seen[partiqlRow] ?: 0
            if (remaining > 0) {
                seen[partiqlRow] = remaining - 1
                continue
            }
            return Record.of(*partiqlRow)
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
            val partiqlRow = row.toDatumArrayCoerceMissing()
            val n = seen[partiqlRow] ?: 0
            seen[partiqlRow] = n + 1
        }
    }
}
