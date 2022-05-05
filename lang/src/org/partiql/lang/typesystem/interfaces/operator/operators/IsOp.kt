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
abstract class IsOp : PqlOperator {
    override fun getOperatorAlias(): OpAlias = OpAlias.IS

    /**
     * Type of the source expression
     */
    abstract fun getSourceType(): Type

    /**
     * Target type
     */
    abstract fun getExpectedType(): Type

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
     * [paramRegistry] is used to get type parameters of operands
     */
    abstract fun invoke(sourceValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry
     */
    data class ParameterRegistry internal constructor(
        val parametersOfSourceType: TypeParameters,
        val parametersOfExpectedType: TypeParameters
    )
}
