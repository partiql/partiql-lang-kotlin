package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.SqlOperator
import org.partiql.lang.typesystem.interfaces.type.CompileTimeType
import org.partiql.lang.typesystem.interfaces.type.ScalarType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

sealed class BinaryOp(override val operatorAlias: OpAlias) : SqlOperator {
    /**
     * Type of left-hand side expression
     */
    abstract val lhsType: ScalarType

    /**
     * Type of right-hand side expression
     */
    abstract val rhsType: ScalarType

    /**
     * Function return type inference
     *
     * [paramRegistry] is the registry of type parameters of operands.
     */
    abstract fun inferReturnType(paramRegistry: ParameterRegistry): CompileTimeType

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

abstract class AndOp : BinaryOp(OpAlias.AND)
abstract class ConcatOp : BinaryOp(OpAlias.CONCAT)
abstract class DivideOp : BinaryOp(OpAlias.DIVIDE)
abstract class MinusOp : BinaryOp(OpAlias.MINUS)
abstract class ModuloOp : BinaryOp(OpAlias.MODULO)
abstract class OrOp : BinaryOp(OpAlias.OR)
abstract class PlusOp : BinaryOp(OpAlias.PLUS)
abstract class TimesOp : BinaryOp(OpAlias.TIMES)
