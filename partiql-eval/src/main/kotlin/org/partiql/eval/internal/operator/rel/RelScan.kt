package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
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
            is CollectionValue<*> -> r.map { Record.of(it) }.iterator()
            else -> {
                close()
                throw TypeCheckException()
            }
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
