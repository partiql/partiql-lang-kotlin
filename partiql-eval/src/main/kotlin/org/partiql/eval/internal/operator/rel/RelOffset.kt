package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValueExperimental
import java.math.BigInteger

@OptIn(PartiQLValueExperimental::class)
internal class RelOffset(
    private val input: Operator.Relation,
    private val offset: Operator.Expr,
) : Operator.Relation {

    private var init = false
    private var _seen: BigInteger = BigInteger.ZERO
    private var _offset: BigInteger = BigInteger.ZERO

    override fun open() {
        input.open()
        init = false
        _seen = BigInteger.ZERO

        // TODO pass outer scope to offset expression
        val o = offset.eval(Record.empty)
        if (o is NumericValue<*>) {
            _offset = o.toInt().value!!
        } else {
            throw TypeCheckException()
        }
    }

    override fun next(): Record? {
        if (!init) {
            while (_seen < _offset) {
                input.next() ?: return null
                _seen = _seen.add(BigInteger.ONE)
            }
            init = true
        }
        return input.next()
    }

    override fun close() {
        input.close()
    }
}
