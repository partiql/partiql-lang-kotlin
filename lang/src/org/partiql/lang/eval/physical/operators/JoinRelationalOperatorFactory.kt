package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.EvaluatorState

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Join] operator.*/
abstract class JoinRelationalOperatorFactory(name: String) : RelationalOperatorFactory {
    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.JOIN, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Join]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Type of join, i.e. inner, left, right, outer. */
        joinType: PartiqlPhysical.JoinType,
        /** Invokes the left-side bindings expression of the join.*/
        leftBexpr: RelationExpression,
        /** Invokes the right-side bindings expression of the join.*/
        rightBexpr: RelationExpression,
        /** Invokes the join's predicate expression. */
        predicateExpr: ValueExpression?,
        /** Sets the left-side variables to `null`. Needed for right joins when there are no rows matching the predicate. */
        setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
        /** Sets the right-side variables to `null`. Needed for left joins when there are no rows matching the predicate. */
        setRightSideVariablesToNull: (EvaluatorState) -> Unit
    ): RelationExpression
}
