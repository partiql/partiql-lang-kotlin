package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

/**
 * Non-communicative, this performs better when [lhs] is larger than [rhs].
 *
 * @property lhs
 * @property rhs
 */
internal class RelExceptDistinct(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

    private var seen: MutableSet<Record> = mutableSetOf()
    private var init: Boolean = false

    override fun openPeeking(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        init = false
        seen = mutableSetOf()
    }

    override fun peek(): Record? {
        if (!init) {
            seed()
        }
        for (row in lhs) {
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
        while (true) {
            if (rhs.hasNext().not()) {
                break
            }
            val row = rhs.next()
            seen.add(row)
        }
    }
}
