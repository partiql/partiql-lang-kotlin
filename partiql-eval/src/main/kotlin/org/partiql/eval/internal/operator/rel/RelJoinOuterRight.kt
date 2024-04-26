package org.partiql.eval.internal.operator.rel

import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.TypesUtility.toRuntimeType
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Rel
import org.partiql.value.PartiQLValueExperimental

internal class RelJoinOuterRight(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
    private val condition: Operator.Expr,
    lhsType: Rel.Type
) : RelPeeking() {

    @OptIn(PartiQLValueExperimental::class)
    private val lhsPadded = Record(
        Array(lhsType.schema.size) { PQLValue.nullValue(lhsType.schema[it].type.toRuntimeType()) }
    )

    private lateinit var env: Environment
    private lateinit var iterator: Iterator<Record>

    override fun open(env: Environment) {
        this.env = env
        rhs.open(env)
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
     * RIGHT OUTER JOIN (LATERAL)
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
     *
     * Another Note: For some databases, RIGHT OUTER JOIN cannot be used with LATERAL. This should be taken care of
     * at planning time. TODO: Should we support LATERAL here? Can we deal with this at planning time?
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun implementation() = iterator {
        for (rhsRecord in rhs) {
            var rhsMatched = false
            lhs.open(env.push(rhsRecord))
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
