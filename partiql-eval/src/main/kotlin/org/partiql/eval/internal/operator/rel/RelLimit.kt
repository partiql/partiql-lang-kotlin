package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.ValueUtility.getBigIntCoerced
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import java.math.BigInteger

@OptIn(PartiQLValueExperimental::class)
internal class RelLimit(
    private val input: Operator.Relation,
    private val limit: Operator.Expr,
) : Operator.Relation {

    private var _seen: BigInteger = BigInteger.ZERO
    private var _limit: BigInteger = BigInteger.ZERO

    override fun open(env: Environment) {
        input.open(env)
        _seen = BigInteger.ZERO

        val l = limit.eval(env.push(Record.empty))
        _limit = l.getBigIntCoerced()
    }

    override fun hasNext(): Boolean {
        return _seen < _limit && input.hasNext()
    }

    override fun next(): Record {
        val row = input.next()
        _seen = _seen.add(BigInteger.ONE)
        return row
    }

    override fun close() {
        input.close()
    }
}
