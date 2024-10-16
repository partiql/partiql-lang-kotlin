package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.rel.RelType
import org.partiql.spi.value.Datum

/**
 * Left Outer Join returns all joined records from the [lhs] and [rhs] when the [condition] evaluates to true. For all
 * records from the [lhs] that do not evaluate to true, these are also returned along with a NULL record from the [rhs].
 *
 * Note: This is currently the lateral version of the left outer join. In the future, the two implementations
 * (lateral vs non-lateral) may be separated for performance improvements.
 */
internal class RelOpJoinOuterLeft(
    private val env: Environment,
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
    private val condition: Operator.Expr,
    rhsType: RelType,
) : RelOpPeeking() {

    // TODO BETTER MECHANISM FOR NULL PADDING
    private val rhsPadded = Row(rhsType.getFields().map { Datum.nullValue(it.type) }.toTypedArray())
    private lateinit var iterator: Iterator<Row>

    override fun openPeeking() {
        lhs.open()
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
     * LEFT OUTER JOIN (LATERAL)
     *
     * Algorithm:
     * ```
     * for lhsRecord in lhs:
     *   for rhsRecord in rhs(lhsRecord):
     *     if (condition matches):
     *       conditionMatched = true
     *       yield(lhsRecord + rhsRecord)
     *   if (!conditionMatched):
     *     yield(lhsRecord + NULL_RECORD)
     * ```
     *
     * Development Note: The non-lateral version wouldn't need to push to the current environment.
     */
    private fun implementation() = iterator {
        for (lhsRecord in lhs) {
            var lhsMatched = false
            rhs.open()
            for (rhsRecord in rhs) {
                val row = lhsRecord + rhsRecord
                val result = env.scope(row) { condition.eval() }
                if (result.isTrue()) {
                    lhsMatched = true
                    yield(row)
                }
            }
            rhs.close()
            if (!lhsMatched) {
                yield(lhsRecord + rhsPadded)
            }
        }
    }
}
