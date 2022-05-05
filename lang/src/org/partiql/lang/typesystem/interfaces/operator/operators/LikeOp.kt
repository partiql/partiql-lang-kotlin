package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

/**
 * Used to define [OpAlias.LIKE] operator
 */
abstract class LikeOp : PqlOperator {
    override fun getOperatorAlias(): OpAlias = OpAlias.LIKE

    /**
     * Type of the source expression
     */
    abstract fun getSourceType(): Type

    /**
     * Type of the matching pattern
     */
    abstract fun getPatternType(): Type

    /**
     * Type of the escaping characters
     */
    abstract fun getEscapeType(): Type

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
     * [pattern] is the value of the matching pattern
     * [escape] is the value of escaping characters
     * [paramRegistry] is used to get type parameters of operands
     */
    abstract fun invoke(sourceValue: ExprValue, pattern: ExprValue, escape: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry
     */
    data class ParameterRegistry internal constructor(
        val parametersOfSourceType: TypeParameters,
        val parametersOfPatternType: TypeParameters,
        val parametersOfEscapeType: TypeParameters
    )
}
