package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelUnionAll(
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
            true -> lhs.next()
            false -> rhs.next()
        }
    }

    override fun close() {
        lhs.close()
        rhs.close()
    }
}
