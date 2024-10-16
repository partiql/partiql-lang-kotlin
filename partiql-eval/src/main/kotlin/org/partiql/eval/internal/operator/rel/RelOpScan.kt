package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.RecordValueIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.types.PType

internal class RelOpScan(
    private val expr: Operator.Expr,
) : Operator.Relation {

    private lateinit var records: Iterator<Row>

    override fun open() {
        val r = expr.eval()
        records = when (r.type.kind) {
            PType.Kind.ARRAY, PType.Kind.BAG, PType.Kind.SEXP -> RecordValueIterator(r.iterator())
            else -> {
                close()
                throw TypeCheckException("Unexpected type for scan: ${r.type}")
            }
        }
    }

    override fun hasNext(): Boolean = records.hasNext()

    override fun next(): Row {
        return records.next()
    }

    override fun close() {}
}
