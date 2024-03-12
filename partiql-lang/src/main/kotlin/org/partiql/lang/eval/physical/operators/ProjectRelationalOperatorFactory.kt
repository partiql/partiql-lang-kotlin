package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Project] operator.*/
@Deprecated("To be removed in the next major version.", replaceWith = ReplaceWith("ProjectRelationalOperatorFactoryAsync"))
abstract class ProjectRelationalOperatorFactory(name: String) : RelationalOperatorFactory {
    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.PROJECT, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Project]. */
    @Deprecated("To be removed in the next major version.", replaceWith = ReplaceWith("ProjectRelationalOperatorFactoryAsync.create"))
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invoke to set the binding for the current row. */
        setVar: SetVariableFunc,
        /** Invoke to obtain evaluation-time arguments. */
        args: List<ValueExpression>
    ): RelationExpression
}
