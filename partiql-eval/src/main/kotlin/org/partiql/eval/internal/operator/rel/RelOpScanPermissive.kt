package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumUtils.lowerSafe
import org.partiql.eval.internal.helpers.RecordValueIterator
import org.partiql.spi.types.PType

internal class RelOpScanPermissive(
    private val expr: ExprValue
) : ExprRelation {

    private lateinit var records: Iterator<Row>

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Row())).lowerSafe()
        records = when (r.type.code()) {
            PType.BAG, PType.ARRAY -> RecordValueIterator(r.iterator())
            else -> iterator { yield(Row(arrayOf(r))) }
        }
    }

    override fun hasNext(): Boolean {
        return records.hasNext()
    }

    override fun next(): Row {
        return records.next()
    }

    override fun close() {}
}
