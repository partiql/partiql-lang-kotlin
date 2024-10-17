package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.ValueUtility.getBigIntCoerced
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Relation
import java.math.BigInteger

internal class RelOpLimit(
    private val input: Relation,
    private val limit: Expression,
) : Relation {

    private var _seen: BigInteger = BigInteger.ZERO
    private var _limit: BigInteger = BigInteger.ZERO

    override fun open(env: Environment) {
        input.open(env)
        _seen = BigInteger.ZERO

        val l = limit.eval(env.push(Row()))
        _limit = l.getBigIntCoerced() // TODO: The planner should handle the coercion
    }

    override fun hasNext(): Boolean {
        return _seen < _limit && input.hasNext()
    }

    override fun next(): Row {
        val row = input.next()
        _seen = _seen.add(BigInteger.ONE)
        return row
    }

    override fun close() {
        input.close()
    }
}
