package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Scan] operator.*/
public abstract class UnpivotOperatorFactory(name: String) : RelationalOperatorFactory {
    public final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.UNPIVOT, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Scan]. */
    public abstract fun create(
        /** Invoke to obtain the value to be iterated over.*/
        expr: ValueExpression,
        /** Invoke to set the `AS` variable binding. */
        setAsVar: SetVariableFunc,
        /** Invoke to set the `AT` variable binding, if non-null */
        setAtVar: SetVariableFunc?,
        /** Invoke to set the `BY` variable binding, if non-null. */
        setByVar: SetVariableFunc?
    ): RelationExpression
}
