package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.operator.Record
import org.partiql.eval.operator.Relation
import java.util.TreeSet

internal class RelOpIntersectDistinct(
    private val lhs: Relation,
    private val rhs: Relation,
) : RelOpPeeking() {

    private val seen = TreeSet(DatumArrayComparator)
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
            if (seen.remove(row.values)) {
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
            seen.add(row.values)
        }
    }
}
