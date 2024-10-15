package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Record
import org.partiql.eval.operator.Relation
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class RelOpIteratePermissive(
    private val expr: Expression
) : Relation {

    private lateinit var iterator: Iterator<Datum>
    private var index: Long = 0
    private var isIndexable: Boolean = true

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Record()))
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

    override fun next(): Record {
        val v = iterator.next()
        return when (isIndexable) {
            true -> {
                val i = index
                index += 1
                Record(arrayOf(v, Datum.bigint(i)))
            }
            false -> Record(arrayOf(v, Datum.missing()))
        }
    }

    override fun close() {}
}
