package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.internal.operator.Operator
import java.util.TreeSet

internal class RelOpIntersectDistinct(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelOpPeeking() {

    private val seen = TreeSet(DatumArrayComparator)
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
        for (row in rhs) {
            row.values.coerceMissing()
            if (seen.remove(row.values)) {
                return Row(row.values)
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
            seen.add(row.values)
        }
    }
}
