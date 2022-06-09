package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Project] operator.*/
abstract class ProjectPhysicalOperatorFactory(name: String) : PhysicalOperatorFactory {
    final override val key: PhysicalOperatorFactoryKey = PhysicalOperatorFactoryKey(PhysicalOperatorKind.PROJECT, name)

    /** Creates a [BindingsExpr] instance for [PartiqlPhysical.Bexpr.Project]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invoke to set the binding for the current row. */
        setVar: SetVariableFunc,
        /** Invoke to obtain evaluation-time arguments. */
        args: List<ValueExpr>
    ): BindingsExpr
}
