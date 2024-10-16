package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator

/**
 * The filter operator needs a reference to the stack because it needs to push the current row before evaluating
 * the predicate. That row is returned iff the predicate is true.
 *
 * @property env
 * @property input
 * @property expr
 */
internal class RelOpFilter(
    private val env: Environment,
    private val input: Operator.Relation,
    private val expr: Operator.Expr,
) : RelOpPeeking() {

    override fun openPeeking() {
        input.open()
    }

    override fun peek(): Row? {
        for (row in input) {
            env.scope(row) {
                if (expr.eval().isTrue()) {
                    return row
                }
            }
        }
        return null
    }

    override fun closePeeking() {
        input.close()
    }
}
