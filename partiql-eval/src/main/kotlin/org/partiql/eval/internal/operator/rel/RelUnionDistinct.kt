package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorChain
import org.partiql.eval.internal.operator.Operator

internal class RelUnionDistinct(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

    private val seen: MutableSet<Record> = mutableSetOf()
    private lateinit var input: Iterator<Record>

    override fun open(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        seen.clear()
        input = IteratorChain(listOf(lhs, rhs))
        super.open(env)
    }

    override fun peek(): Record? {
        for (record in input) {
            if (!seen.contains(record)) {
                seen.add(record)
                return record
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
}
