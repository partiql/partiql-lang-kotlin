package org.partiql.lang.eval.physical.operators

import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation

internal fun createCrossJoinRelItr(
    leftBexpr: BindingsExpr,
    rightBexpr: BindingsExpr,
    state: EvaluatorState
): RelationIterator {
    return relation(RelationType.BAG) {
        val leftItr = leftBexpr(state)
        while (leftItr.nextRow()) {
            val rightItr = rightBexpr(state)
            while (rightItr.nextRow()) {
                yield()
            }
        }
    }
}

/**
 * Like [createCrossJoinRelItr], but the right-hand relation is padded with unknown values in the event
 * that it is empty or that the predicate does not match.
 */
internal fun createLeftJoinRelItr(
    leftBexpr: BindingsExpr,
    rightBexpr: BindingsExpr,
    setRightSideVariablesToNull: (EvaluatorState) -> Unit,
    predicateExpr: ValueExpr?,
    state: EvaluatorState
): RelationIterator {
    return if (predicateExpr == null) {
        relation(RelationType.BAG) {
            val leftItr = leftBexpr(state)
            while (leftItr.nextRow()) {
                val rightItr = rightBexpr(state)
                // if the rightItr does has a row...
                if (rightItr.nextRow()) {
                    yield() // yield current row
                    yieldAll(rightItr) // yield remaining rows
                } else {
                    // no row--yield padded row
                    setRightSideVariablesToNull(state)
                    yield()
                }
            }
        }
    } else {
        relation(RelationType.BAG) {
            val leftItr = leftBexpr(state)
            while (leftItr.nextRow()) {
                val rightItr = rightBexpr(state)
                var yieldedSomething = false
                while (rightItr.nextRow()) {
                    if (coercePredicateResult(predicateExpr(state))) {
                        yield()
                        yieldedSomething = true
                    }
                }
                // If we still haven't yielded anything, we still need to emit a row with right-hand side variables
                // padded with unknowns.
                if (!yieldedSomething) {
                    setRightSideVariablesToNull(state)
                    yield()
                }
            }
        }
    }
}
