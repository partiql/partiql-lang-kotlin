package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.SqlTypeWithParameters
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

abstract class BinaryOp internal constructor() : SqlOperator {
    /**
     * Type of left-hand side expression
     */
    abstract val lhsType: SqlType

    /**
     * Type of right-hand side expression
     */
    abstract val rhsType: SqlType

    /**
     * Function return type inference
     *
     * [paramRegistry] is the registry of type parameters of operands.
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): SqlTypeWithParameters

    /**
     * Evaluation
     *
     * [lhsValue] is the value of left-hand side passed to this operator at evaluation time.
     * [rhsValue] is the value of right-hand side passed to this operator at evaluation time.
     * [paramRegistry] is the registry of type parameters of operands.
     */
    abstract fun invoke(lhsValue: ExprValue, rhsValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry. Type parameters are registered during compile time.
     */
    data class ParameterRegistry internal constructor(
        val parametersOfLhsType: TypeParameters,
        val parametersOfRhsType: TypeParameters
    )
}
