package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

abstract class UnaryOp internal constructor() : PqlOperator {
    /**
     * Type of expression
     */
    abstract fun getExprType(): Type

    /**
     * Function return type inference
     *
     * [paramRegistry] is used to get type parameters of the operand
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): List<TypeWithParameters>

    /**
     * Evaluation
     *
     * [sourceValue] is the value of the source expression
     * [paramRegistry] is used to get type parameters of the operand
     */
    abstract fun invoke(sourceValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry
     */
    data class ParameterRegistry(
        val parametersOfExprType: TypeParameters
    )
}
