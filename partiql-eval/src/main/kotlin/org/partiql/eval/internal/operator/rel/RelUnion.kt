package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelUnion(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : Operator.Relation {

    override fun open() {
        lhs.open()
        rhs.open()
    }

    override fun next(): Record? {
        val l = lhs.next()
        if (l != null) {
            return l
        }
        return rhs.next()
    }

    override fun close() {
        lhs.close()
        rhs.close()
    }
}
