package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.operator.Record

internal class RelOpUnionAll(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : Operator.Relation {

    override fun open(env: Environment) {
        lhs.open(env)
        rhs.open(env)
    }

    override fun hasNext(): Boolean {
        return lhs.hasNext() || rhs.hasNext()
    }

    override fun next(): Record {
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
