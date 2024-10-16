package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.internal.operator.Operator

/**
 * Inner Join returns all joined records from the [lhs] and [rhs] when the [condition] evaluates to true.
 *
 * Note: This is currently the lateral version of the inner join. In the future, the two implementations
 * (lateral vs non-lateral) may be separated for performance improvements.
 */
internal class RelOpJoinInner(
    private val env: Environment,
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
    private val condition: Operator.Expr,
) : RelOpPeeking() {

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
     * INNER JOIN (LATERAL)
     *
     * Algorithm:
     * ```
     * for l in lhs:
     *   for r in rhs(lhsRecord):
     *     if (condition matches):
     *       conditionMatched = true
     *       yield(l + r)
     * ```
     *
     * Development Note: The non-lateral version wouldn't need to push to the current environment.
     */
    private fun implementation() = iterator {
        for (l in lhs) {
            env.push(l)
            rhs.open()
            env.pop() // possible bug? the planner assumes the left-scope is dropped and pushes (l+r).
            // afaik there should be two scopes [<l>, <r>] rather than [<l+r>] since l introduces its own scope.
            for (r in rhs) {
                val row = l + r
                env.push(row)
                val result = condition.eval()
                env.pop()
                if (result.isTrue()) {
                    yield(row)
                }
            }
        }
    }
}
