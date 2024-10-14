package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.operator.Record
import org.partiql.plan.rel.RelType
import org.partiql.spi.value.Datum

/**
 * Full Outer Join returns all joined records from the [lhs] and [rhs] when the [condition] evaluates to true. For all
 * records from the [lhs] that do not evaluate to true, these are also returned along with a NULL record from the [rhs].
 * For all records from the [rhs] that do not evaluate to true, these are also returned along with a NULL record from the [lhs].
 *
 * Full Outer Join cannot be lateral according to PartiQL Specification Section 5.5.
 */
internal class RelOpJoinOuterFull(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
    private val condition: Operator.Expr,
    lhsType: RelType,
    rhsType: RelType,
) : RelOpPeeking() {

    // TODO BETTER MECHANISM FOR NULL PADDING
    private val r = rhsType.getFields().toTypedArray()
    private val l = lhsType.getFields().toTypedArray()
    private val lhsPadded: Record = Record(l.indices.map { Datum.nullValue(l[it].type) }.toTypedArray())
    private val rhsPadded: Record = Record(r.indices.map { Datum.nullValue(r[it].type) }.toTypedArray())

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
     * FULL OUTER JOIN (CANNOT BE LATERAL)
     *
     * Merge Join (special implementation for FULL OUTER). This is used because we don't have a sophisticated enough
     * planner to perform the transformation specified by SQL Server: see section
     * ["What about full outer joins?"](https://learn.microsoft.com/en-us/archive/blogs/craigfr/nested-loops-join).
     * Furthermore, SQL Server allows for merge joins even without an equijoin predicate. See section
     * ["Outer and semi-joins"](https://learn.microsoft.com/en-us/archive/blogs/craigfr/merge-join).
     *
     *
     * Algorithm:
     * ```
     * for lhsRecord, lhsIndex in lhs_sorted:
     *   for rhsRecord, rhsIndex in rhs_sorted:
     *     if (condition matches):
     *       lhsMatches[lhsIndex] = true
     *       rhsMatches[rhsIndex] = true
     *       yield(lhsRecord + rhsRecord)
     * for lhsRecord, lhsIndex in lhs_sorted:
     *   if lhsMatches[lhsIndex] = false:
     *     yield(lhsRecord, null)
     * for rhsRecord, rhsIndex in rhs_sorted:
     *   if rhsMatches[rhsIndex] = false:
     *     yield(null, rhsRecord)
     * ```
     *
     * TODO: Merge joins require that the LHS and RHS are sorted.
     */
    private fun implementation() = iterator {
        val lhsMatches = mutableSetOf<Int>()
        val rhsMatches = mutableSetOf<Int>()
        for ((lhsIndex, lhsRecord) in lhs.withIndex()) {
            rhs.open(env)
            for ((rhsIndex, rhsRecord) in rhs.withIndex()) {
                val input = lhsRecord.concat(rhsRecord)
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    lhsMatches.add(lhsIndex)
                    rhsMatches.add(rhsIndex)
                    yield(lhsRecord.concat(rhsRecord))
                }
            }
            rhs.close()
        }
        lhs.close()
        lhs.open(env)
        for ((lhsIndex, lhsRecord) in lhs.withIndex()) {
            if (!lhsMatches.contains(lhsIndex)) {
                yield(lhsRecord.concat(rhsPadded))
            }
        }
        lhs.close()
        rhs.open(env)
        for ((rhsIndex, rhsRecord) in rhs.withIndex()) {
            if (!rhsMatches.contains(rhsIndex)) {
                yield(lhsPadded.concat(rhsRecord))
            }
        }
    }
}
