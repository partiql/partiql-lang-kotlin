package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class RelLimit(
    private val input: Operator.Relation,
    private val limit: Operator.Expr,
) : Operator.Relation {

    private var _seen: Long = 0
    private var _limit: Long = 0

    override fun open() {
        input.open()
        _seen = 0

        // TODO pass outer scope to limit expression
        val l = limit.eval(Record.empty)
        if (l is NumericValue<*>) {
            _limit = l.toInt64().value ?: 0L
        } else {
            throw TypeCheckException()
        }
    }

    override fun next(): Record? {
        if (_seen < _limit) {
            val row = input.next() ?: return null
            _seen += 1
            return row
        }
        return null
    }

    override fun close() {
        input.close()
    }
}
