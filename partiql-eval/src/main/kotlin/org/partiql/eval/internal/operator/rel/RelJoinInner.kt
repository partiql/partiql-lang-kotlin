package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental

internal class RelJoinInner(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
    private val condition: Operator.Expr,
) : RelPeeking() {

    private lateinit var env: Environment
    private lateinit var iterator: Iterator<Record>

    override fun open(env: Environment) {
        this.env = env
        lhs.open(env)
        iterator = implementation()
        super.open(env)
    }

    override fun peek(): Record? {
        return when (iterator.hasNext()) {
            true -> iterator.next()
            false -> null
        }
    }

    override fun close() {
        lhs.close()
        rhs.close()
        iterator = emptyList<Record>().iterator()
        super.close()
    }

    /**
     * INNER JOIN (LATERAL)
     *
     * Algorithm:
     * ```
     * for lhsRecord in lhs:
     *   for rhsRecord in rhs(lhsRecord):
     *     if (condition matches):
     *       conditionMatched = true
     *       yield(lhsRecord + rhsRecord)
     * ```
     *
     * Development Note: The non-lateral version wouldn't need to push to the current environment.
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun implementation() = iterator {
        for (lhsRecord in lhs) {
            rhs.open(env.push(lhsRecord))
            for (rhsRecord in rhs) {
                val input = lhsRecord + rhsRecord
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    yield(lhsRecord + rhsRecord)
                }
            }
        }
    }
}
