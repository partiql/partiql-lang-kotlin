package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

@OptIn(PartiQLValueExperimental::class)
internal class RelScanIndexed(
    private val expr: Operator.Expr
) : Operator.Relation {

    private lateinit var iterator: Iterator<PQLValue>
    private var index: Long = 0

    override fun open(env: Environment) {
        val r = expr.eval(env.push(Record.empty))
        index = 0
        iterator = when (r.type) {
            PartiQLValueType.BAG -> {
                close()
                throw TypeCheckException()
            }
            PartiQLValueType.LIST, PartiQLValueType.SEXP -> r.iterator()
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
        return Record.of(v, PQLValue.int64Value(i))
    }

    override fun close() {}
}
