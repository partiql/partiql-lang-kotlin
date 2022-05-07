package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.PqlOperator
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

/**
 * Used to define [OpAlias.LIKE] operator
 */
abstract class LikeOp : PqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.LIKE

    /**
     * Type of the source expression
     */
    abstract val sourceType: SqlType

    /**
     * Type of the matching pattern
     */
    abstract val patternType: SqlType

    /**
     * Type of the escaping characters
     */
    abstract val escapeType: SqlType

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
     * [patternValue] is the value of the matching pattern passed to this operator during evaluation time.
     * [escapeValue] is the value of escaping characters passed to this operator during evaluation time.
     * [paramRegistry] is the registry of type parameters.
     */
    abstract fun invoke(sourceValue: ExprValue, patternValue: ExprValue, escapeValue: ExprValue, paramRegistry: ParameterRegistry): ExprValue

    /**
     * Type parameters registry. Type parameters are registered during compile time.
     */
    data class ParameterRegistry internal constructor(
        val parametersOfSourceType: TypeParameters,
        val parametersOfPatternType: TypeParameters,
        val parametersOfEscapeType: TypeParameters
    )
}
