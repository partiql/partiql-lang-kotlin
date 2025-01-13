package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import java.util.TreeMap

internal class RelOpExceptAll(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
) : RelOpPeeking() {

    private val seen = TreeMap<Row, Int>(DatumArrayComparator)
    private var init: Boolean = false

    override fun openPeeking(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        init = false
        seen.clear()
    }

    override fun peek(): Row? {
        if (!init) {
            seed()
        }
        for (row in lhs) {
            row.coerceMissing()
            val remaining = seen[row] ?: 0
            if (remaining > 0) {
                seen[row] = remaining - 1
                continue
            }
            return row
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
            row.coerceMissing()
            val n = seen[row] ?: 0
            seen[row] = n + 1
        }
    }
}
