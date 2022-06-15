package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Offset] operator.*/
abstract class OffsetRelationalOperatorFactory(name: String) : RelationalOperatorFactory {
    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.OFFSET, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Offset]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invokes the row count expression. */
        rowCountExpr: ValueExpression,
        /** Invokes the source bindings expression. */
        sourceBexpr: RelationExpression
    ): RelationExpression
}
