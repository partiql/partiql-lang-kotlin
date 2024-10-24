package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.ValueUtility.isTrue

/**
 * Inner Join returns all joined records from the [lhs] and [rhs] when the [condition] evaluates to true.
 *
 * Note: This is currently the lateral version of the inner join. In the future, the two implementations
 * (lateral vs non-lateral) may be separated for performance improvements.
 */
internal class RelOpJoinInner(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
    private val condition: ExprValue,
) : RelOpPeeking() {

    private lateinit var env: Environment
    private lateinit var iterator: Iterator<Row>

    override fun openPeeking(env: Environment) {
        this.env = env
        lhs.open(env)
        iterator = implementation()
    }

    override fun peek(): Row? {
        return when (iterator.hasNext()) {
            true -> iterator.next()
            false -> null
        }
    }

    override fun closePeeking() {
        lhs.close()
        rhs.close()
        iterator = emptyList<Row>().iterator()
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
    private fun implementation() = iterator {
        for (lhsRecord in lhs) {
            rhs.open(env.push(lhsRecord))
            for (rhsRecord in rhs) {
                val input = lhsRecord.concat(rhsRecord)
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    yield(lhsRecord.concat(rhsRecord))
                }
            }
        }
    }
}
