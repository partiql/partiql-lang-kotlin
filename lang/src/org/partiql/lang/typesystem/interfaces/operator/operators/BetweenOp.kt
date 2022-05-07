package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

/**
 * Used to define [OpAlias.BETWEEN] operator
 */
abstract class BetweenOp : PqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.BETWEEN

    /**
     * Type of the source expression
     */
    abstract val sourceType: SqlType

    /**
     * Type of the "from" expression
     */
    abstract val fromType: SqlType

    /**
     * Type of the "to" expression
     */
    abstract val toType: SqlType

    /**
     * Function return type inference
     *
     * [paramRegistry] is the registry of type parameters of operands.
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): TypeWithParameters

    /**
     * Evaluation
     *
     * [sourceValue] is the value of the source expression passed to this operator at evaluation time.
     * [fromValue] is the value of "from" expression passed to this operator at evaluation time.
     * [toValue] is the value of "to" expression passed to this operator at evaluation time.
     * [paramRegistry] is the registry of type parameters of operands.
     */
    abstract fun invoke(sourceValue: ExprValue, fromValue: ExprValue, toValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry. Type parameters are registered during compile time.
     */
    data class ParameterRegistry internal constructor(
        val parametersOfSourceType: TypeParameters,
        val parametersOfFromType: TypeParameters,
        val parametersOfToType: TypeParameters
    )
}
