package org.partiql.eval.internal.operator.rel

import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

@OptIn(PartiQLValueExperimental::class)
internal class RelScanIndexedPermissive(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var iterator: Iterator<PQLValue>
    private var index: Long = 0
    private var isIndexable: Boolean = true

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Record.empty))
        index = 0
        iterator = when (r.type) {
            PartiQLValueType.BAG -> {
                isIndexable = false
                r.bagValues
            }
            PartiQLValueType.LIST -> r.listValues
            PartiQLValueType.SEXP -> r.sexpValues
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
                Record.of(v, PQLValue.int64Value(i))
            }
            false -> Record.of(v, PQLValue.missingValue())
        }
    }

    override fun close() {}
}
