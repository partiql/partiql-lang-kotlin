package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelIntersect(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelMaterialized() {

    private var seen: MutableSet<Record> = mutableSetOf()
    private var init: Boolean = false

    override fun open(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        init = false
        seen = mutableSetOf()
    }

    override fun materializeNext(): Record? {
        if (!init) {
            seed()
        }
        while (rhs.hasNext()) {
            val row = rhs.next()
            if (seen.contains(row)) {
                return row
            }
        }
        return null
    }

    override fun close() {
        lhs.close()
        rhs.close()
        seen.clear()
    }

    /**
     * Read the entire left-hand-side into our search structure.
     */
    private fun seed() {
        init = true
        while (true) {
            val row = lhs.next() ?: break
            seen.add(row)
        }
    }
}
