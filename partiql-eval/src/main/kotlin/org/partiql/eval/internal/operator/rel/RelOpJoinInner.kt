package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.helpers.checkInterrupted

/**
 * Non-lateral inner join. Both sides are opened independently. The RHS is materialized once and
 * rescanned for each LHS row. The condition is evaluated on the combined row.
 */
internal class RelOpJoinInner(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
    private val condition: ExprValue,
) : RelOpPeeking() {

    private lateinit var env: Environment
    private lateinit var iterator: Iterator<Row>
    private lateinit var rhsRows: List<Row>

    override fun openPeeking(env: Environment) {
        this.env = env
        lhs.open(env)
        rhs.open(env)
        // Materialize RHS so we can rescan per LHS row
        rhsRows = mutableListOf<Row>().also { list ->
            for (row in rhs) { list.add(row) }
        }
        rhs.close()
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
        iterator = emptyList<Row>().iterator()
    }

    /**
     * INNER JOIN (NON-LATERAL)
     *
     * Algorithm:
     * ```
     * rhsRows = materialize(rhs)
     * for lhsRecord in lhs:
     *   for rhsRecord in rhsRows:
     *     if (condition matches):
     *       yield(lhsRecord + rhsRecord)
     * ```
     */
    private fun implementation() = iterator {
        for (lhsRecord in lhs) {
            for (rhsRecord in rhsRows) {
                checkInterrupted()
                val input = lhsRecord.concat(rhsRecord)
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    yield(lhsRecord.concat(rhsRecord))
                }
            }
        }
    }
}
