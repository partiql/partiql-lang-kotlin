package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

abstract class UnaryOp internal constructor() : PqlOperator {
    /**
     * Type of expression
     */
    abstract val exprType: Type

    /**
     * Evaluation
     *
     * [source] is the value with assigned type of the source expression
     */
    abstract fun invoke(source: ValueWithType): ValueWithType
}
