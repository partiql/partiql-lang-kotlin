package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

/**
 * Used to define [OpAlias.IN] operator
 */
abstract class InOp: PqlOperator {
    override fun getOperatorAlias(): OpAlias = OpAlias.IN

    /**
     * Type of the source expression
     */
    abstract fun getSourceType(): Type

    /**
     * Type of the collection
     */
    abstract fun getCollectionType(): Type

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
     * [collection] is the value of the collection expression
     * [paramRegistry] is used to get type parameters of operands
     */
    abstract fun invoke(sourceValue: ExprValue, collection: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry
     */
    data class ParameterRegistry(
        val parametersOfSourceType: TypeParameters,
        val parametersOfCollectionType: TypeParameters
    )
}