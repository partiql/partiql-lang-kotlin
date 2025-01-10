package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing

internal class RelOpUnionAll(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
) : ExprRelation {

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
                record.coerceMissing()
                record
            }
            false -> {
                val record = rhs.next()
                record.coerceMissing()
                record
            }
        }
    }

    override fun close() {
        lhs.close()
        rhs.close()
    }
}
