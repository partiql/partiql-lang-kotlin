package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

abstract class BinaryOp internal constructor() : PqlOperator {
    /**
     * Type of left-hand side expression
     */
    abstract fun getLhsType(): Type

    /**
     * Type of right-hand side expression
     */
    abstract fun getRhsType(): Type

    /**
     * Function return type inference
     *
     * [paramRegistry] is used to get type parameters of operands
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): List<TypeWithParameters>

    /**
     * Evaluation
     *
     * [lhsValue] represents value of left-hand side
     * [rhsValue] represents value of right-hand side
     * [paramRegistry] is used to get type parameters of operands
     */
    abstract fun invoke(lhsValue: ExprValue, rhsValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry
     */
    data class ParameterRegistry(
        val parametersOfLhsType: TypeParameters,
        val parametersOfRhsType: TypeParameters
    )
}
