package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.SqlTypeWithParameters
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

abstract class AbstractCastOp internal constructor() : SqlOperator {
    /**
     * Type of the source expression
     */
    abstract val sourceType: SqlType

    /**
     * Target type
     */
    abstract val targetType: SqlType

    /**
     * Infer return type
     *
     * [paramRegistry] is the registry of type parameters of operands.
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): SqlTypeWithParameters

    /**
     * Evaluation
     *
     * [sourceValue] is the value of the source expression passed to this operator during evaluation time.
     * [paramRegistry] is the registry of type parameters of operands.
     */
    abstract fun invoke(sourceValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry. Type parameters are registered during compile time.
     */
    data class ParameterRegistry internal constructor(
        val parametersOfSourceType: TypeParameters,
        val parametersOfTargetType: TypeParameters
    )
}
