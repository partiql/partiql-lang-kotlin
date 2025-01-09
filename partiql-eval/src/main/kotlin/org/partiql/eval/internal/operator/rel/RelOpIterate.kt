package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumUtils.lowerSafe
import org.partiql.spi.errors.TypeCheckException
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class RelOpIterate(
    private val expr: ExprValue
) : ExprRelation {

    private lateinit var iterator: Iterator<Datum>
    private var index: Long = 0

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Row())).lowerSafe()
        index = 0
        iterator = when (r.type.code()) {
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
