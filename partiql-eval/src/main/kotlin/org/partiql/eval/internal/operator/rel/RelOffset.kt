package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
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

    override fun open(env: Environment) {
        input.open(env)
        init = false
        _seen = BigInteger.ZERO

        val o = offset.eval(env.push(Record.empty))
        if (o is NumericValue<*>) {
            _offset = o.toInt().value!!
        } else {
            throw TypeCheckException()
        }
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
