package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.RecordValueIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

@OptIn(PartiQLValueExperimental::class)
internal class RelScan(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var records: Iterator<Record>

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Record.empty))
        records = when (r.type) {
            PartiQLValueType.LIST -> RecordValueIterator(r.listValues)
            PartiQLValueType.BAG -> RecordValueIterator(r.bagValues)
            PartiQLValueType.SEXP -> RecordValueIterator(r.sexpValues)
            else -> {
                close()
                throw TypeCheckException()
            }
        }
    }

    override fun hasNext(): Boolean = records.hasNext()

    override fun next(): Record {
        return records.next()
    }

    override fun close() {}
}
