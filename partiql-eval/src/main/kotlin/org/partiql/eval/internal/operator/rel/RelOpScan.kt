package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumUtils.lowerSafe
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.RecordValueIterator
import org.partiql.spi.types.PType

internal class RelOpScan(
    private val expr: ExprValue
) : ExprRelation {

    private lateinit var records: Iterator<Row>

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Row())).lowerSafe()
        records = when (r.type.code()) {
            PType.ARRAY, PType.BAG -> RecordValueIterator(r.iterator())
            else -> {
                close()
                throw PErrors.collectionExpectedException(r.type)
            }
        }
    }

    override fun hasNext(): Boolean = records.hasNext()

    override fun next(): Row {
        return records.next()
    }

    override fun close() {}
}
