package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.internal.operator.Operator

internal class RelOpUnionAll(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : Operator.Relation {

    override fun open() {
        lhs.open()
        rhs.open()
    }

    override fun hasNext(): Boolean {
        return lhs.hasNext() || rhs.hasNext()
    }

    override fun next(): Row {
        return when (lhs.hasNext()) {
            true -> {
                val record = lhs.next()
                record.values.coerceMissing()
                record
            }
            false -> {
                val record = rhs.next()
                record.values.coerceMissing()
                record
            }
        }
    }

    override fun close() {
        lhs.close()
        rhs.close()
    }
}
