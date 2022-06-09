package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Limit] operator.*/
abstract class LimitPhysicalOperatorFactory(name: String) : PhysicalOperatorFactory {
    final override val key: PhysicalOperatorFactoryKey = PhysicalOperatorFactoryKey(PhysicalOperatorKind.LIMIT, name)

    /** Creates a [BindingsExpr] instance for [PartiqlPhysical.Bexpr.Limit]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invokes the row count expression. */
        rowCountExpr: ValueExpr,
        /** Invokes the source bindings expression. */
        sourceBexpr: BindingsExpr
    ): BindingsExpr
}
