package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Row
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class RelOpIterate(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var iterator: Iterator<Datum>
    private var index: Long = 0

    override fun open() {
        val r = expr.eval()
        index = 0
        iterator = when (r.type.kind) {
            PType.Kind.BAG -> {
                close()
                throw TypeCheckException()
            }
            PType.Kind.ARRAY, PType.Kind.SEXP -> r.iterator()
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
        return Row.of(v, Datum.bigint(i))
    }

    override fun close() {}
}
