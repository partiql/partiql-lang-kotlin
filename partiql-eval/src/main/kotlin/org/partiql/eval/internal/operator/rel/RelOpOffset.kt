package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.ValueUtility.getBigIntCoerced
import org.partiql.eval.internal.operator.Operator
import java.math.BigInteger

internal class RelOpOffset(
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

        val o = offset.eval()
        _offset = o.getBigIntCoerced() // TODO: The planner should handle the coercion
    }

    override fun hasNext(): Boolean {
        if (!init) {
            while (input.hasNext()) {
                if (_seen >= _offset) {
                    break
                }
                _seen = _seen.add(BigInteger.ONE)
                input.next()
            }
            init = true
        }
        return input.hasNext()
    }

    override fun next(): Row {
        return input.next()
    }

    override fun close() {
        input.close()
    }
}
