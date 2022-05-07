package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

/**
 * Used to define [OpAlias.IS] operator
 */
abstract class IsOp : PqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.IS

    /**
     * Type of the source expression
     */
    abstract val sourceType: SqlType

    /**
     * Target type
     */
    abstract val expectedType: SqlType

    /**
     * Function return type inference
     *
     * [paramRegistry] is the registry of type parameters.
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): TypeWithParameters

    /**
     * Evaluation
     *
     * [sourceValue] is the value of the source expression passed to this operator during evaluation time.
     * [paramRegistry] is the registry of type parameters.
     */
    abstract fun invoke(sourceValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry. Type parameters are registered during compile time.
     */
    data class ParameterRegistry internal constructor(
        val parametersOfSourceType: TypeParameters,
        val parametersOfExpectedType: TypeParameters
    )
}
