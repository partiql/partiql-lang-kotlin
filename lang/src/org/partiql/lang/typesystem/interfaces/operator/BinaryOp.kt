package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

abstract class BinaryOp internal constructor() : PqlOperator {
    /**
     * Type of left-hand side expression
     */
    abstract val lhsType: Type

    /**
     * Type of right-hand side expression
     */
    abstract val rhsType: Type

    /**
     * Evaluation
     *
     * [lhs] represents value with assigned type of left-hand side
     * [rhs] represents value with assigned type of right-hand side
     */
    abstract fun invoke(lhs: ValueWithType, rhs: ValueWithType): ValueWithType
}
