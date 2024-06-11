package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

@OptIn(PartiQLValueExperimental::class)
internal class RelScanIndexedPermissive(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var iterator: Iterator<Datum>
    private var index: Long = 0
    private var isIndexable: Boolean = true

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Record.empty))
        index = 0
        iterator = when (r.type) {
            PartiQLValueType.BAG -> {
                isIndexable = false
                r.iterator()
            }
            PartiQLValueType.LIST, PartiQLValueType.SEXP -> r.iterator()
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
                Record.of(v, Datum.int64Value(i))
            }
            false -> Record.of(v, Datum.missingValue())
        }
    }

    override fun close() {}
}
