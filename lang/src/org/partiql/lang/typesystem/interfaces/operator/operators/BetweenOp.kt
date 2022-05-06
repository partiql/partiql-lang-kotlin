package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

/**
 * Used to define [OpAlias.IS] operator
 */
abstract class BetweenOp : PqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.BETWEEN

    /**
     * Type of the source expression
     */
    abstract val sourceType: Type

    /**
     * Type of the "from" expression
     */
    abstract val fromType: Type

    /**
     * Type of the "to" expression
     */
    abstract val toType: Type

    /**
     * Evaluation
     *
     * [source] is the value with assigned type of the source expression
     * [from] is the value with assigned type of "from" expression
     * [to] is the value with assigned type of "to" expression
     */
    abstract fun invoke(source: ValueWithType, from: ValueWithType, to: ValueWithType): ValueWithType
}
