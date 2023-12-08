package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.CollectionValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class RelScan(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var records: Iterator<Record>

    override fun open() {
        val r = expr.eval(Record.empty)
        records = when (r) {
            is CollectionValue<*> -> r.elements!!.map { Record.of(it) }.iterator()
            else -> iterator { yield(Record.of(r)) }
        }
    }

    override fun next(): Record? {
        return if (records.hasNext()) {
            records.next()
        } else {
            null
        }
    }

    override fun close() {}
}
