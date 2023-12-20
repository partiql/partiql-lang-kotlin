package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelDistinct(
    val input: Operator.Relation
) : Operator.Relation {

    private val seen = mutableSetOf<Record>()

    override fun open() {
        input.open()
    }

    override fun next(): Record? {
        var next = input.next()
        while (next != null) {
            if (seen.contains(next).not()) {
                seen.add(next)
                return next
            }
            next = input.next()
        }
        return null
    }

    override fun close() {
        input.close()
    }
}
