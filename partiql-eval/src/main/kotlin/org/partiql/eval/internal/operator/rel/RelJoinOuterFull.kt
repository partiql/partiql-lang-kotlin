package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

/**
 * Here's a simple implementation of FULL OUTER JOIN. The idea is fairly straightforward:
 * Iterate through LHS. For each iteration of the LHS, iterate through RHS. Now, check the condition.
 * - If the condition passes, return the merged record (equivalent to result of INNER JOIN)
 * - If the condition does not pass, we need a way to return two records (one where the LHS is padded with nulls, and
 *   one where the RHS is padded with nulls). How we do this:
 *   - We maintain the [previousLhs] and [previousRhs]. If they are null, we then compute the next LHS and RHS. We
 *     store their values in-memory. Then we return a merged Record where the LHS is padded and the RHS is not (equivalent
 *     to result of RIGHT OUTER JOIN).
 *   - If they aren't null, then we pad the RHS with NULLS (we assume we've already padded the LHS) and return (equivalent
 *     to result of LEFT OUTER JOIN). We also make sure [previousLhs] and [previousRhs] are now null.
 *
 * Performance Analysis: Assume that [lhs] has size M and [rhs] has size N.
 * - Time: O(M * N)
 * - Space: O(1)
 */
internal class RelJoinOuterFull(
    override val lhs: Operator.Relation,
    override val rhs: Operator.Relation,
    override val condition: Operator.Expr
) : RelJoinNestedLoop() {

    private var previousLhs: Record? = null
    private var previousRhs: Record? = null

    override fun next(): Record? {
        if (previousLhs != null && previousRhs != null) {
            previousRhs!!.padNull()
            val newRecord = previousLhs!! + previousRhs!!
            previousLhs = null
            previousRhs = null
            return newRecord
        }
        return super.next()
    }

    /**
     * Specifically, for FULL OUTER JOIN, when the JOIN Condition ([condition]) is TRUE, we need to return the
     * rows merged (without modification). When the JOIN Condition ([condition]) is FALSE, we need to return
     * the LHS padded (and merged with RHS not padded) and the RHS padded (merged with the LHS not padded).
     */
    override fun join(condition: Boolean, lhs: Record, rhs: Record): Record {
        when (condition) {
            true -> {
                previousLhs = null
                previousRhs = null
            }
            false -> {
                previousLhs = lhs.copy()
                previousRhs = rhs.copy()
                lhs.padNull()
            }
        }
        return lhs + rhs
    }
}
