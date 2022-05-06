package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

abstract class AbstractCastOp internal constructor() : PqlOperator {
    /**
     * Type of the source expression
     */
    abstract val sourceType: Type

    /**
     * Target type
     */
    abstract val targetType: Type

    /**
     * Evaluation
     *
     * [source] is the value of the source expression
     * [expectedType] is the expected sql type
     */
    abstract fun invoke(source: ValueWithType, expectedType: TypeWithParameters): ValueWithType
}
