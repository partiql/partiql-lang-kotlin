package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BagValue
import org.partiql.value.CollectionValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int64Value

@OptIn(PartiQLValueExperimental::class)
internal class RelScanIndexed(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var iterator: Iterator<PartiQLValue>
    private var index: Long = 0

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Record.empty))
        index = 0
        iterator = when (r) {
            is BagValue<*> -> {
                close()
                throw TypeCheckException()
            }
            is CollectionValue<*> -> r.iterator()
            else -> {
                close()
                throw TypeCheckException()
            }
        }
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): Record {
        val i = index
        val v = iterator.next()
        index += 1
        return Record.of(v, int64Value(i))
    }

    override fun close() {}
}
