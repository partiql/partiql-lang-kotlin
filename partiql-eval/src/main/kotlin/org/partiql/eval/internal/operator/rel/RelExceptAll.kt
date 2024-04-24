package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelExceptAll(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

    private val seen: MutableMap<Record, Int> = mutableMapOf()
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
            val n = seen[row] ?: 0
            seen[row] = n + 1
        }
    }
}
