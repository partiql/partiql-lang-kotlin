package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Filter] operator.*/
abstract class FilterPhysicalOperatorFactory(name: String) : PhysicalOperatorFactory {
    final override val key: PhysicalOperatorFactoryKey = PhysicalOperatorFactoryKey(PhysicalOperatorKind.FILTER, name)

    /** Creates a [BindingsExpr] instance for [PartiqlPhysical.Bexpr.Filter]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invokes the filter's predicate. */
        predicate: ValueExpr,
        /** Invokes the source binding expression. */
        sourceBexpr: BindingsExpr
    ): BindingsExpr
}
