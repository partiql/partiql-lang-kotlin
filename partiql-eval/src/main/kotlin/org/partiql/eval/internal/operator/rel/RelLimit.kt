package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelLimit(
    private val input: Operator.Relation,
    private val limit: Long,
) : Operator.Relation {

    private var seen = 0

    override fun open() {
        input.open()
        seen = 0
    }

    override fun next(): Record? {
        if (seen < limit) {
            val row = input.next() ?: return null
            seen += 1
            return row
        }
        return null
    }

    override fun close() {
        input.close()
    }
}