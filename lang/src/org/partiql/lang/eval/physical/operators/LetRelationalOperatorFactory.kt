package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc

/** A compiled variable binding. */
class VariableBinding(
    /** The function to be invoked at evaluation-time to set the value of the variable. */
    val setFunc: SetVariableFunc,
    /** The function to be invoked at evaluation-time to compute the value of the variable. */
    val expr: ValueExpression
)

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Let] operator.*/
abstract class LetRelationalOperatorFactory(name: String) : RelationalOperatorFactory {
    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.LET, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Let]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invokes the source bindings expression. */
        sourceBexpr: RelationExpression,
        /** List of [VariableBinding]s that were included in the `LET` clause. */
        bindings: List<VariableBinding>
    ): RelationExpression
}
