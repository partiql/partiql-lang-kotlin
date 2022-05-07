package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

/**
 * Used to define [OpAlias.IN] operator
 */
abstract class InOp : PqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.IN

    /**
     * Type of the source expression
     */
    abstract val sourceType: SqlType

    /**
     * Type of the collection
     */
    abstract val collectionType: SqlType

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
     * [collection] is the value of the collection expression passed to this operator during evaluation time.
     * [paramRegistry] is the registry of type parameters.
     */
    abstract fun invoke(sourceValue: ExprValue, collection: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry. Type parameters are registered during compile time.
     */
    data class ParameterRegistry(
        val parametersOfSourceType: TypeParameters,
        val parametersOfCollectionType: TypeParameters
    )
}
