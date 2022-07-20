package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.SqlOperator
import org.partiql.lang.typesystem.interfaces.type.CompileTimeType
import org.partiql.lang.typesystem.interfaces.type.ScalarType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

sealed class CastOp : SqlOperator {
    override val operatorAlias = OpAlias.CAST

    /**
     * Type of the source expression
     */
    abstract val sourceType: ScalarType

    /**
     * Target type
     */
    abstract val targetType: ScalarType

    /**
     * Infer return type
     *
     * [paramRegistry] is the registry of type parameters of operands.
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): CompileTimeType

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
