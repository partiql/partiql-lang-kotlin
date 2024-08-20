package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.plan.Rel
import org.partiql.value.PartiQLValueExperimental

/**
 * Left Outer Join returns all joined records from the [lhs] and [rhs] when the [condition] evaluates to true. For all
 * records from the [lhs] that do not evaluate to true, these are also returned along with a NULL record from the [rhs].
 *
 * Note: This is currently the lateral version of the left outer join. In the future, the two implementations
 * (lateral vs non-lateral) may be separated for performance improvements.
 */
internal class RelOpJoinOuterLeft(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
    private val condition: Operator.Expr,
    rhsType: Rel.Type
) : RelOpPeeking() {

    private val rhsPadded = Record(
        Array(rhsType.schema.size) { Datum.nullValue(rhsType.schema[it].type) }
    )

    private lateinit var env: Environment
    private lateinit var iterator: Iterator<Record>

    override fun openPeeking(env: Environment) {
        this.env = env
        lhs.open(env)
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
    @OptIn(PartiQLValueExperimental::class)
    private fun implementation() = iterator {
        for (lhsRecord in lhs) {
            var lhsMatched = false
            rhs.open(env.push(lhsRecord))
            for (rhsRecord in rhs) {
                val input = lhsRecord + rhsRecord
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    lhsMatched = true
                    yield(lhsRecord + rhsRecord)
                }
            }
            rhs.close()
            if (!lhsMatched) {
                yield(lhsRecord + rhsPadded)
            }
        }
    }
}
