package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.RecordValueIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.operator.Record
import org.partiql.types.PType

internal class RelOpScanPermissive(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var records: Iterator<Record>

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Record()))
        records = when (r.type.kind) {
            PType.Kind.BAG, PType.Kind.ARRAY, PType.Kind.SEXP -> RecordValueIterator(r.iterator())
            else -> iterator { yield(Record(arrayOf(r))) }
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
