package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getBigIntCoerced
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Record
import org.partiql.eval.operator.Relation
import java.math.BigInteger

/**
 * A reasonably complicated and realistic example of a custom operator.
 */
class CustomOperatorTest {

    /**
     * Custom LIMIT+OFFSET operator.
     */
    private class LimitOffset(
        private val input: Relation,
        private val limit: Expression,
        private val offset: Expression,
    ) : Relation {

        private var init = false
        private var _seen: BigInteger = BigInteger.ZERO
        private var _offset: BigInteger = BigInteger.ZERO
        private var _limit: BigInteger = BigInteger.ZERO

        override fun open(env: Environment) {
            input.open(env)

            val e = env.push(Record())
            val l = limit.eval(e)
            val o = offset.eval(e)
            _offset = o.getBigIntCoerced()
            _limit = l.getBigIntCoerced()
        }

        override fun hasNext(): Boolean {
            TODO("Not yet implemented")
        }

        override fun next(): Record {
            return input.next()
        }

        override fun close() {
            input.close()
        }
    }

    @Test
    fun testCustomOperator() {
        TODO()
    }
}
