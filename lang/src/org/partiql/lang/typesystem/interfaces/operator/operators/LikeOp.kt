package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.SqlOperator
import org.partiql.lang.typesystem.interfaces.type.CompileTimeType
import org.partiql.lang.typesystem.interfaces.type.ScalarType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

/**
 * Used to define [OpAlias.LIKE] operator
 */
abstract class LikeOp : SqlOperator {
    override val operatorAlias: OpAlias
        get() = OpAlias.LIKE

    /**
     * Type of the source expression
     */
    abstract val sourceType: ScalarType

    /**
     * Type of the matching pattern
     */
    abstract val patternType: ScalarType

    /**
     * Type of the escaping characters
     */
    abstract val escapeType: ScalarType

    /**
     * Function return type inference
     *
     * [paramRegistry] is the registry of type parameters.
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): CompileTimeType

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
