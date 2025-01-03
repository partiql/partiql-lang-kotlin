package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.plan.rel.RelType
import org.partiql.spi.value.Datum

/**
 * Right Outer Join returns all joined records from the [lhs] and [rhs] when the [condition] evaluates to true. For all
 * records from the [rhs] that do not evaluate to true, these are also returned along with a NULL record from the [lhs].
 *
 * Right Outer Join cannot be lateral according to PartiQL Specification Section 5.5.
 */
internal class RelOpJoinOuterRight(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
    private val condition: ExprValue,
    lhsType: RelType
) : RelOpPeeking() {

    // TODO BETTER MECHANISM FOR NULL PADDING
    private val lhsPadded =
        Row(
            lhsType.getFields().map { Datum.nullValue(it.type) }
                .toTypedArray()
        )

    private lateinit var env: Environment
    private lateinit var iterator: Iterator<Row>

    override fun openPeeking(env: Environment) {
        this.env = env
        rhs.open(env)
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
                val input = lhsRecord.concat(rhsRecord)
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    rhsMatched = true
                    yield(lhsRecord.concat(rhsRecord))
                }
            }
            lhs.close()
            if (!rhsMatched) {
                yield(lhsPadded.concat(rhsRecord))
            }
        }
    }
}
