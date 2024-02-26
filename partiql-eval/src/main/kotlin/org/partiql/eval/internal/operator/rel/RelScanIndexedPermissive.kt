package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BagValue
import org.partiql.value.CollectionValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int64Value
import org.partiql.value.missingValue

@OptIn(PartiQLValueExperimental::class)
internal class RelScanIndexedPermissive(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var iterator: Iterator<PartiQLValue>
    private var index: Long = 0
    private var isIndexable: Boolean = true

    override fun open() {
        val r = expr.eval(Record.empty)
        index = 0
        iterator = when (r) {
            is BagValue<*> -> {
                isIndexable = false
                r.iterator()
            }
            is CollectionValue<*> -> r.iterator()
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
                Record.of(v, int64Value(i))
            }
            false -> Record.of(v, missingValue())
        }
    }

    override fun close() {}
}
