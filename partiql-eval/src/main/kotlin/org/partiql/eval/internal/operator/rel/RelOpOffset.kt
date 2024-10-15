package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getBigIntCoerced
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Record
import org.partiql.eval.operator.Relation
import java.math.BigInteger

internal class RelOpOffset(
    private val input: Relation,
    private val offset: Expression,
) : Relation {

    private var init = false
    private var _seen: BigInteger = BigInteger.ZERO
    private var _offset: BigInteger = BigInteger.ZERO

    override fun open(env: Environment) {
        input.open(env)
        init = false
        _seen = BigInteger.ZERO

        val o = offset.eval(env.push(Record()))
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

    override fun next(): Record {
        return input.next()
    }

    override fun close() {
        input.close()
    }
}
