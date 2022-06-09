package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Scan] operator.*/
abstract class ScanPhysicalOperatorFactory(name: String) : PhysicalOperatorFactory {
    final override val key: PhysicalOperatorFactoryKey = PhysicalOperatorFactoryKey(PhysicalOperatorKind.SCAN, name)

    /** Creates a [BindingsExpr] instance for [PartiqlPhysical.Bexpr.Scan]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invoke to obtain the value to be iterated over.*/
        expr: ValueExpr,
        /** Invoke to set the `AS` variable binding. */
        setAsVar: SetVariableFunc,
        /** Invoke to set the `AT` variable binding, if non-null. */
        setAtVar: SetVariableFunc?,
        /** Invoke to set the `BY` variable binding, if non-null. */
        setByVar: SetVariableFunc?
    ): BindingsExpr
}
