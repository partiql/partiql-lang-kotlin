package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class RelOpIterate(
    private val expr: ExprValue
) : ExprRelation {

    private lateinit var iterator: Iterator<Datum>
    private var index: Long = 0

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Row()))
        index = 0
        iterator = records(r)
    }

    private fun records(r: Datum): Iterator<Datum> {
        return when (r.type.code()) {
            PType.VARIANT -> records(r.lower())
            PType.BAG -> {
                close()
                throw TypeCheckException()
            }
            PType.ARRAY -> r.iterator()
            else -> {
                close()
                throw TypeCheckException()
            }
        }
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): Row {
        val i = index
        val v = iterator.next()
        index += 1
        return Row(arrayOf(v, Datum.bigint(i)))
    }

    override fun close() {}
}
