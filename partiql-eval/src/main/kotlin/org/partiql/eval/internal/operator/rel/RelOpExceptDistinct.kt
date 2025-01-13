package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import java.util.TreeSet

/**
 * Non-communicative, this performs better when [lhs] is larger than [rhs].
 *
 * @property lhs
 * @property rhs
 */
internal class RelOpExceptDistinct(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
) : RelOpPeeking() {

    private var seen = TreeSet(DatumArrayComparator)
    private var init: Boolean = false

    override fun openPeeking(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        init = false
    }

    override fun peek(): Row? {
        if (!init) {
            seed()
        }
        for (row in lhs) {
            row.coerceMissing()
            if (!seen.contains(row)) {
                return row
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
     * Read the entire right-hand-side into our search structure.
     */
    private fun seed() {
        init = true
        for (row in rhs) {
            row.coerceMissing()
            seen.add(row)
        }
    }
}
