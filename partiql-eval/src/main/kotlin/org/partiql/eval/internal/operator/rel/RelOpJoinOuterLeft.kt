package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.helpers.checkInterrupted
import org.partiql.plan.rel.RelType
import org.partiql.spi.value.Datum

/**
 * Non-lateral left outer join. Both sides are opened independently. The RHS is materialized once
 * and rescanned for each LHS row. LHS rows with no match are preserved with NULL-padded RHS.
 */
internal class RelOpJoinOuterLeft(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
    private val condition: ExprValue,
    rhsType: RelType,
) : RelOpPeeking() {

    private val rhsPadded = Row(
        rhsType.getFields().map { Datum.nullValue(it.type) }.toTypedArray()
    )

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
     * LEFT OUTER JOIN (NON-LATERAL)
     *
     * Algorithm:
     * ```
     * rhsRows = materialize(rhs)
     * for lhsRecord in lhs:
     *   matched = false
     *   for rhsRecord in rhsRows:
     *     if (condition matches):
     *       matched = true
     *       yield(lhsRecord + rhsRecord)
     *   if (!matched):
     *     yield(lhsRecord + NULL_RECORD)
     * ```
     */
    private fun implementation() = iterator {
        for (lhsRecord in lhs) {
            var lhsMatched = false
            for (rhsRecord in rhsRows) {
                checkInterrupted()
                val input = lhsRecord.concat(rhsRecord)
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    lhsMatched = true
                    yield(lhsRecord.concat(rhsRecord))
                }
            }
            if (!lhsMatched) {
                yield(lhsRecord.concat(rhsPadded))
            }
        }
    }
}
