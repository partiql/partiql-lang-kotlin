package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

/**
 * Used to define [OpAlias.IN] operator
 */
abstract class InOp : PqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.IN

    /**
     * Type of the source expression
     */
    abstract val sourceType: Type

    /**
     * Type of the collection
     */
    abstract val collectionType: Type

    /**
     * Evaluation
     *
     * [source] is the value with assigned type of the source expression
     * [collection] is the value with assigned type of the collection expression
     */
    abstract fun invoke(source: ValueWithType, collection: ValueWithType): ValueWithType
}
