package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

abstract class AbstractCastOp internal constructor(): PqlOperator {
    /**
     * Type of the source expression
     */
    abstract fun getSourceType(): Type

    /**
     * Target type
     */
    abstract fun getTargetType(): Type

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
    data class ParameterRegistry(
        val parametersOfSourceType: TypeParameters,
        val parametersOfTargetType: TypeParameters
    )
}