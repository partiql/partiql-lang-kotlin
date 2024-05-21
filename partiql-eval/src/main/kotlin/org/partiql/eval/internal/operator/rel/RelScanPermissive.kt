package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.RecordValueIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

@OptIn(PartiQLValueExperimental::class)
internal class RelScanPermissive(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var records: Iterator<Record>

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Record.empty))
        records = when (r.type) {
            PartiQLValueType.BAG, PartiQLValueType.LIST, PartiQLValueType.SEXP -> RecordValueIterator(r.iterator())
            else -> iterator { yield(Record.of(r)) }
        }
    }

    override fun hasNext(): Boolean {
        return records.hasNext()
    }

    override fun next(): Record {
        return records.next()
    }

    override fun close() {}
}
