package org.partiql.lang.eval.physical.operators

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.util.interruptibleFold

/** Creates a new [RelationIterator] that returns a cross join of [leftRel] and [rightRel]. */
internal fun createCrossJoinRelItr(
    leftRel: RelationExpression,
    rightRel: RelationExpression,
    state: EvaluatorState
): RelationIterator {
    return relation(RelationType.BAG) {
        val leftItr = leftRel.evaluate(state)
        while (leftItr.nextRow()) {
            val rightItr = rightRel.evaluate(state)
            while (rightItr.nextRow()) {
                yield()
            }
        }
    }
}

/**
 * Like [createCrossJoinRelItr], but the right-hand relation is padded with unknown values in the event
 * that it is empty or that the predicate does not match.
 *
 * This can also be used to perform right joins: simply swap left and right.
 */
internal fun createLeftJoinRelItr(
    leftRel: RelationExpression,
    rightRel: RelationExpression,
    setRightSideVariablesToNull: (EvaluatorState) -> Unit,
    predicateExpr: ValueExpression?,
    state: EvaluatorState
): RelationIterator {
    return if (predicateExpr == null) {
        relation(RelationType.BAG) {
            val leftItr = leftRel.evaluate(state)
            while (leftItr.nextRow()) {
                val rightItr = rightRel.evaluate(state)
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
            val leftItr = leftRel.evaluate(state)
            while (leftItr.nextRow()) {
                val rightItr = rightRel.evaluate(state)
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

/** Creates a new relation from  that filters rows from [relItr] given the [predicate]. */
internal fun createFilterRelItr(
    relItr: RelationIterator,
    predicate: ValueExpression,
    state: EvaluatorState
) = relation(RelationType.BAG) {
    while (true) {
        if (!relItr.nextRow()) {
            break
        } else {
            val matches = predicate.invoke(state)
            if (coercePredicateResult(matches)) {
                yield()
            }
        }
    }
}

/**
 * Coerces [value] into a [Boolean] value as appropriate for a `WHERE` clause or `JOIN` predicate.
 *
 * That is, `NULL` or `MISSING` values are equivalent to `false`.
 */
private fun coercePredicateResult(value: ExprValue): Boolean =
    when {
        value.isUnknown() -> false
        else -> value.booleanValue() // <-- throws if [value] is not a boolean.
    }

internal fun getSortingComparator(sortKeys: List<CompiledSortKey>, state: EvaluatorState): Comparator<Array<ExprValue>> {
    val initial: Comparator<Array<ExprValue>>? = null
    return sortKeys.interruptibleFold(initial) { intermediate, sortKey ->
        if (intermediate == null) {
            return@interruptibleFold compareBy<Array<ExprValue>, ExprValue>(sortKey.comparator) { row ->
                transferState(state, row)
                sortKey.value(state)
            }
        }
        return@interruptibleFold intermediate.thenBy(sortKey.comparator) { row ->
            transferState(state, row)
            sortKey.value(state)
        }
    } ?: err(
        "Order BY comparator cannot be null",
        ErrorCode.EVALUATOR_ORDER_BY_NULL_COMPARATOR,
        null,
        internal = true
    )
}

internal fun transferState(target: EvaluatorState, source: Array<ExprValue>) {
    if (target.registers.size != source.size) {
        throw EvaluationException("Something Wrong", ErrorCode.EVALUATOR_GENERIC_EXCEPTION, null, null, true)
    }
    target.registers.forEachIndexed { index, _ -> target.registers[index] = source[index] }
}
