package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelIntersectDistinct(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

    private val seen: MutableSet<Record> = mutableSetOf()
    private var init: Boolean = false

    override fun open(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        init = false
        seen.clear()
        super.open(env)
    }

    override fun peek(): Record? {
        if (!init) {
            seed()
        }
        for (row in rhs) {
            if (seen.remove(row)) {
                return row
            }
        }
        return null
    }

    override fun close() {
        lhs.close()
        rhs.close()
        seen.clear()
        super.close()
    }

    /**
     * Read the entire left-hand-side into our search structure.
     */
    private fun seed() {
        init = true
        for (row in lhs) {
            seen.add(row)
        }
    }
}
