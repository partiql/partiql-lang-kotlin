package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

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
     * Function return type inference
     *
     * [paramRegistry] is used to get type parameters of operands
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): TypeWithParameters

    /**
     * Evaluation
     *
     * [sourceValue] is the value of the source expression
     * [fromValue] is the value of "from" expression
     * [toValue] is the value of "to" expression
     * [paramRegistry] is used to get type parameters of operands
     */
    abstract fun invoke(sourceValue: ExprValue, fromValue: ExprValue, toValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry
     */
    data class ParameterRegistry internal constructor(
        val parametersOfSourceType: TypeParameters,
        val parametersOfFromType: TypeParameters,
        val parametersOfToType: TypeParameters
    )
}
