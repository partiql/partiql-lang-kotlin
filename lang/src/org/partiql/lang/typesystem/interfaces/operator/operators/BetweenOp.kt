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
    override fun getOperatorAlias(): OpAlias = OpAlias.BETWEEN

    /**
     * Type of the source expression
     */
    abstract fun getSourceType(): Type

    /**
     * Type of the "from" expression
     */
    abstract fun getFromType(): Type

    /**
     * Type of the "to" expression
     */
    abstract fun getToType(): Type

    /**
     * Function return type inference
     *
     * [paramRegistry] is used to get type parameters of operands
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): List<TypeWithParameters>

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
    data class ParameterRegistry(
        val parametersOfSourceType: TypeParameters,
        val parametersOfFromType: TypeParameters,
        val parametersOfToType: TypeParameters
    )
}
