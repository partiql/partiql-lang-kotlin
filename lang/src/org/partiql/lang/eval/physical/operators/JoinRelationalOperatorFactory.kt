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
abstract class JoinRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.JOIN, name)

    /**
     * Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Join].
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
        leftBexpr: RelationExpression,
        rightBexpr: RelationExpression,
        predicateExpr: ValueExpression?,
        setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
        setRightSideVariablesToNull: (EvaluatorState) -> Unit
    ): RelationExpression
}

internal object JoinRelationalOperatorFactoryDefault : JoinRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        joinType: PartiqlPhysical.JoinType,
        leftBexpr: RelationExpression,
        rightBexpr: RelationExpression,
        predicateExpr: ValueExpression?,
        setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
        setRightSideVariablesToNull: (EvaluatorState) -> Unit
    ): RelationExpression = when (joinType) {
        is PartiqlPhysical.JoinType.Inner -> {
            InnerJoinOperator(
                lhs = leftBexpr,
                rhs = rightBexpr,
                condition = predicateExpr?.closure() ?: { true }
            )
        }
        is PartiqlPhysical.JoinType.Left -> {
            LeftJoinOperator(
                lhs = leftBexpr,
                rhs = rightBexpr,
                condition = predicateExpr?.closure() ?: { true },
                setRightSideVariablesToNull = setRightSideVariablesToNull
            )
        }
        is PartiqlPhysical.JoinType.Right -> {
            RightJoinOperator(
                lhs = leftBexpr,
                rhs = rightBexpr,
                condition = predicateExpr?.closure() ?: { true },
                setLeftSideVariablesToNull = setLeftSideVariablesToNull
            )
        }
        is PartiqlPhysical.JoinType.Full -> TODO("Full join")
    }

    private fun ValueExpression.closure() = { state: EvaluatorState ->
        val v = invoke(state)
        v.isNotUnknown() && v.booleanValue()
    }
}

/**
 * See specification 5.6
 */
internal class InnerJoinOperator(
    private val lhs: RelationExpression,
    private val rhs: RelationExpression,
    private val condition: (EvaluatorState) -> Boolean
) : RelationExpression {

    override fun evaluate(state: EvaluatorState) = relation(RelationType.BAG) {
        val leftItr = lhs.evaluate(state)
        while (leftItr.nextRow()) {
            val rightItr = rhs.evaluate(state)
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
internal class LeftJoinOperator(
    private val lhs: RelationExpression,
    private val rhs: RelationExpression,
    private val condition: (EvaluatorState) -> Boolean,
    private val setRightSideVariablesToNull: (EvaluatorState) -> Unit
) : RelationExpression {

    override fun evaluate(state: EvaluatorState) = relation(RelationType.BAG) {
        val leftItr = lhs.evaluate(state)
        while (leftItr.nextRow()) {
            val rightItr = rhs.evaluate(state)
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
internal class RightJoinOperator(
    private val lhs: RelationExpression,
    private val rhs: RelationExpression,
    private val condition: (EvaluatorState) -> Boolean,
    private val setLeftSideVariablesToNull: (EvaluatorState) -> Unit
) : RelationExpression {

    override fun evaluate(state: EvaluatorState) = relation(RelationType.BAG) {
        val rightItr = rhs.evaluate(state)
        while (rightItr.nextRow()) {
            val leftItr = lhs.evaluate(state)
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
