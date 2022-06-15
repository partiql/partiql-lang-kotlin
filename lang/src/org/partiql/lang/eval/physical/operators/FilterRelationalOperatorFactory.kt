package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Filter] operator.*/
abstract class FilterRelationalOperatorFactory(name: String) : RelationalOperatorFactory {
    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.FILTER, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Filter]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invokes the filter's predicate. */
        predicate: ValueExpression,
        /** Invokes the source binding expression. */
        sourceBexpr: RelationExpression
    ): RelationExpression
}
