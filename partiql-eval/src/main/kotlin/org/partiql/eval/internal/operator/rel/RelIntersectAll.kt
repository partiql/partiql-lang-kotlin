package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelIntersectAll(
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
        for (row in rhs) {
            seen.computeIfPresent(row) { _, y ->
                when (y) {
                    0 -> null
                    else -> y - 1
                }
            }?.let { return row }
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
            seen.computeIfPresent(row) { _, y ->
                y + 1
            } ?: seen.put(row, 1)
        }
    }
}
