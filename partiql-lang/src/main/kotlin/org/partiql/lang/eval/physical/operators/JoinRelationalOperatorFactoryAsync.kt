package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.isNotUnknown
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

/**
 * Provides an implementation of the [PartiqlPhysical.Bexpr.Join] operator.
 *
 * @constructor
 *
 * @param name
 */
abstract class JoinRelationalOperatorFactoryAsync(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.JOIN, name)

    /**
     * Creates a [RelationExpressionAsync] instance for [PartiqlPhysical.Bexpr.Join].
     *
     * @param impl static arguments
     * @param joinType inner, left, right, outer
     * @param leftBexpr left-hand-side of the join
     * @param rightBexpr right-hand-side of the join
     * @param predicateExpr condition for a theta join
     * @param setLeftSideVariablesToNull
     * @param setRightSideVariablesToNull
     * @return
     */
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        joinType: PartiqlPhysical.JoinType,
        leftBexpr: RelationExpressionAsync,
        rightBexpr: RelationExpressionAsync,
        predicateExpr: ValueExpressionAsync?,
        setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
        setRightSideVariablesToNull: (EvaluatorState) -> Unit
    ): RelationExpressionAsync
}

internal object JoinRelationalOperatorFactoryDefaultAsync : JoinRelationalOperatorFactoryAsync(DEFAULT_IMPL_NAME) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        joinType: PartiqlPhysical.JoinType,
        leftBexpr: RelationExpressionAsync,
        rightBexpr: RelationExpressionAsync,
        predicateExpr: ValueExpressionAsync?,
        setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
        setRightSideVariablesToNull: (EvaluatorState) -> Unit
    ): RelationExpressionAsync = when (joinType) {
        is PartiqlPhysical.JoinType.Inner -> {
            InnerJoinOperatorAsync(
                lhs = leftBexpr,
                rhs = rightBexpr,
                condition = predicateExpr?.closure() ?: { true }
            )
        }
        is PartiqlPhysical.JoinType.Left -> {
            LeftJoinOperatorAsync(
                lhs = leftBexpr,
                rhs = rightBexpr,
                condition = predicateExpr?.closure() ?: { true },
                setRightSideVariablesToNull = setRightSideVariablesToNull
            )
        }
        is PartiqlPhysical.JoinType.Right -> {
            RightJoinOperatorAsync(
                lhs = leftBexpr,
                rhs = rightBexpr,
                condition = predicateExpr?.closure() ?: { true },
                setLeftSideVariablesToNull = setLeftSideVariablesToNull
            )
        }
        is PartiqlPhysical.JoinType.Full -> TODO("Full join")
    }

    private fun ValueExpressionAsync.closure(): suspend (EvaluatorState) -> Boolean = { state: EvaluatorState ->
        val v = invoke(state)
        v.isNotUnknown() && v.booleanValue()
    }
}

/**
 * See specification 5.6
 */
private class InnerJoinOperatorAsync(
    private val lhs: RelationExpressionAsync,
    private val rhs: RelationExpressionAsync,
    private val condition: suspend (EvaluatorState) -> Boolean
) : RelationExpressionAsync {

    override suspend fun evaluateAsync(state: EvaluatorState) = relation(RelationType.BAG) {
        val leftItr = lhs.evaluateAsync(state)
        while (leftItr.nextRow()) {
            val rightItr = rhs.evaluateAsync(state)
            while (rightItr.nextRow()) {
                if (condition(state)) {
                    yield()
                }
            }
        }
    }
}

/**
 * See specification 5.6
 */
private class LeftJoinOperatorAsync(
    private val lhs: RelationExpressionAsync,
    private val rhs: RelationExpressionAsync,
    private val condition: suspend (EvaluatorState) -> Boolean,
    private val setRightSideVariablesToNull: (EvaluatorState) -> Unit
) : RelationExpressionAsync {

    override suspend fun evaluateAsync(state: EvaluatorState) = relation(RelationType.BAG) {
        val leftItr = lhs.evaluateAsync(state)
        while (leftItr.nextRow()) {
            val rightItr = rhs.evaluateAsync(state)
            var yieldedSomething = false
            while (rightItr.nextRow()) {
                if (condition(state)) {
                    yield()
                    yieldedSomething = true
                }
            }
            if (!yieldedSomething) {
                setRightSideVariablesToNull(state)
                yield()
            }
        }
    }
}

/**
 * See specification 5.6
 */
private class RightJoinOperatorAsync(
    private val lhs: RelationExpressionAsync,
    private val rhs: RelationExpressionAsync,
    private val condition: suspend (EvaluatorState) -> Boolean,
    private val setLeftSideVariablesToNull: (EvaluatorState) -> Unit
) : RelationExpressionAsync {

    override suspend fun evaluateAsync(state: EvaluatorState) = relation(RelationType.BAG) {
        val rightItr = rhs.evaluateAsync(state)
        while (rightItr.nextRow()) {
            val leftItr = lhs.evaluateAsync(state)
            var yieldedSomething = false
            while (leftItr.nextRow()) {
                if (condition(state)) {
                    yield()
                    yieldedSomething = true
                }
            }
            if (!yieldedSomething) {
                setLeftSideVariablesToNull(state)
                yield()
            }
        }
    }
}
