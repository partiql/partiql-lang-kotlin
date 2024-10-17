package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.RecordValueIterator
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Relation
import org.partiql.types.PType

internal class RelOpScan(
    private val expr: Expression
) : Relation {

    private lateinit var records: Iterator<Row>

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Row()))
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
