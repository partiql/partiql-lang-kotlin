package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

/**
 * Used to define [OpAlias.LIKE] operator
 */
abstract class LikeOp : PqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.LIKE

    /**
     * Type of the source expression
     */
    abstract val sourceType: Type

    /**
     * Type of the matching pattern
     */
    abstract val patternType: Type

    /**
     * Type of the escaping characters
     */
    abstract val escapeType: Type

    /**
     * Evaluation
     *
     * [source] is the value of the source expression
     * [pattern] is the value of the matching pattern
     * [escape] is the value of escaping characters
     */
    abstract fun invoke(source: ValueWithType, pattern: ValueWithType, escape: ValueWithType): ValueWithType
}
