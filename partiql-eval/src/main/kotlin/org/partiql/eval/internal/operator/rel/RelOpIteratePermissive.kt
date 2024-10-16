package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Row
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class RelOpIteratePermissive(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var iterator: Iterator<Datum>
    private var index: Long = 0
    private var isIndexable: Boolean = true

    override fun open() {
        val r = expr.eval()
        index = 0
        iterator = when (r.type.kind) {
            PType.Kind.BAG -> {
                isIndexable = false
                r.iterator()
            }
            PType.Kind.ARRAY, PType.Kind.SEXP -> r.iterator()
            else -> {
                isIndexable = false
                iterator { yield(r) }
            }
        }
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): Row {
        val v = iterator.next()
        return when (isIndexable) {
            true -> {
                val i = index
                index += 1
                Row.of(v, Datum.bigint(i))
            }
            false -> Row.of(v, Datum.missing())
        }
    }

    override fun close() {}
}
