package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.operator.Relation

internal class RelOpUnionAll(
    private val lhs: Relation,
    private val rhs: Relation,
) : Relation {

    override fun open(env: Environment) {
        lhs.open(env)
        rhs.open(env)
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
