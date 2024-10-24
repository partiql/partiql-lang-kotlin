package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.ValueUtility.getBigIntCoerced
import java.math.BigInteger

internal class RelOpOffset(
    private val input: ExprRelation,
    private val offset: ExprValue,
) : ExprRelation {

    private var init = false
    private var _seen: BigInteger = BigInteger.ZERO
    private var _offset: BigInteger = BigInteger.ZERO

    override fun open(env: Environment) {
        input.open(env)
        init = false
        _seen = BigInteger.ZERO

        val o = offset.eval(env.push(Row()))
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
