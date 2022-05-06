package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

/**
 * Used to define [OpAlias.IS] operator
 */
abstract class IsOp : PqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.IS

    /**
     * Type of the source expression
     */
    abstract val sourceType: Type

    /**
     * Target type
     */
    abstract val expectedType: Type

    /**
     * Evaluation
     *
     * [source] is the value of the source expression
     * [expectedType] is the expected sql type
     */
    abstract fun invoke(source: ValueWithType, expectedType: TypeWithParameters): ValueWithType
}
