package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.plan.Rel

internal class RelJoinOuterRight(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
    private val condition: Operator.Expr,
    lhsType: Rel.Type
) : RelPeeking() {

    private val lhsPadded = Record(
        Array(lhsType.schema.size) { Datum.nullValue(lhsType.schema[it].type) }
    )

    private lateinit var env: Environment
    private lateinit var iterator: Iterator<Record>

    override fun openPeeking(env: Environment) {
        this.env = env
        rhs.open(env)
        iterator = implementation()
    }

    override fun peek(): Record? {
        return when (iterator.hasNext()) {
            true -> iterator.next()
            false -> null
        }
    }

    override fun closePeeking() {
        lhs.close()
        rhs.close()
        iterator = emptyList<Record>().iterator()
    }

    /**
     * RIGHT OUTER JOIN (CANNOT BE LATERAL)
     *
     * Algorithm:
     * ```
     * for rhsRecord in rhs:
     *   for lhsRecord in lhs(rhsRecord):
     *     if (condition matches):
     *       conditionMatched = true
     *       yield(lhsRecord + rhsRecord)
     *   if (!conditionMatched):
     *     yield(NULL_RECORD + rhsRecord)
     * ```
     */
    private fun implementation() = iterator {
        for (rhsRecord in rhs) {
            var rhsMatched = false
            lhs.open(env)
            for (lhsRecord in lhs) {
                val input = lhsRecord + rhsRecord
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    rhsMatched = true
                    yield(lhsRecord + rhsRecord)
                }
            }
            lhs.close()
            if (!rhsMatched) {
                yield(lhsPadded + rhsRecord)
            }
        }
    }
}
