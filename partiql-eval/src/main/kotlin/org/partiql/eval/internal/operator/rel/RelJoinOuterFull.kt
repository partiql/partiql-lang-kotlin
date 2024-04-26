package org.partiql.eval.internal.operator.rel

import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.TypesUtility.toRuntimeType
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Rel
import org.partiql.value.PartiQLValueExperimental

internal class RelJoinOuterFull(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
    private val condition: Operator.Expr,
    lhsType: Rel.Type,
    rhsType: Rel.Type
) : RelPeeking() {

    @OptIn(PartiQLValueExperimental::class)
    private val lhsPadded = Record(
        Array(rhsType.schema.size) { PQLValue.nullValue(lhsType.schema[it].type.toRuntimeType()) }
    )

    @OptIn(PartiQLValueExperimental::class)
    private val rhsPadded = Record(
        Array(rhsType.schema.size) { PQLValue.nullValue(rhsType.schema[it].type.toRuntimeType()) }
    )

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
     * FULL OUTER JOIN (CANNOT BE LATERAL)
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
     * Note: The LHS and RHS must be sorted. TODO: We need to add sorting to the LHS and RHS
     */
    private fun implementation() = iterator {
        val lhsMatches = mutableSetOf<Int>()
        val rhsMatches = mutableSetOf<Int>()
        for ((lhsIndex, lhsRecord) in lhs.withIndex()) {
            rhs.open(env)
            for ((rhsIndex, rhsRecord) in rhs.withIndex()) {
                val input = lhsRecord + rhsRecord
                val result = condition.eval(env.push(input))
                if (result.isTrue()) {
                    lhsMatches.add(lhsIndex)
                    rhsMatches.add(rhsIndex)
                    yield(lhsRecord + rhsRecord)
                }
            }
            rhs.close()
        }
        lhs.close()
        lhs.open(env)
        for ((lhsIndex, lhsRecord) in lhs.withIndex()) {
            if (!lhsMatches.contains(lhsIndex)) {
                yield(lhsRecord + rhsPadded)
            }
        }
        lhs.close()
        rhs.open(env)
        for ((rhsIndex, rhsRecord) in rhs.withIndex()) {
            if (!rhsMatches.contains(rhsIndex)) {
                yield(lhsPadded + rhsRecord)
            }
        }
    }
}
