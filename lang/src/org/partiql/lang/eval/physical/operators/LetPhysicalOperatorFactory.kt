package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc

/** A compiled variable binding. */
class VariableBinding(
    /** The function to be invoked at evaluation-time to set the value of the variable. */
    val setFunc: SetVariableFunc,
    /** The function to be invoked at evaluation-time to compute the value of the variable. */
    val expr: ValueExpr
)

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Let] operator.*/
abstract class LetPhysicalOperatorFactory(name: String) : PhysicalOperatorFactory {
    final override val key: PhysicalOperatorFactoryKey = PhysicalOperatorFactoryKey(PhysicalOperatorKind.LET, name)

    /** Creates a [BindingsExpr] instance for [PartiqlPhysical.Bexpr.Let]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invokes the source bindings expression. */
        sourceBexpr: BindingsExpr,
        /** List of [VariableBinding]s that were included in the `LET` clause. */
        bindings: List<VariableBinding>
    ): BindingsExpr
}
