package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.SqlOperator
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.SqlTypeWithParameters
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

sealed class BinaryOp(override val operatorAlias: OpAlias) : SqlOperator {
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

abstract class AndOp : BinaryOp(OpAlias.AND)
abstract class ConcatOp : BinaryOp(OpAlias.CONCAT)
abstract class DivideOp : BinaryOp(OpAlias.DIVIDE)
abstract class EqOp : BinaryOp(OpAlias.EQ)
abstract class ExceptOp : BinaryOp(OpAlias.EXCEPT)
abstract class GteOp : BinaryOp(OpAlias.GTE)
abstract class GtOp : BinaryOp(OpAlias.GT)
abstract class IntersectOp : BinaryOp(OpAlias.INTERSECT)
abstract class LteOp : BinaryOp(OpAlias.LTE)
abstract class LtOp : BinaryOp(OpAlias.LT)
abstract class MinusOp : BinaryOp(OpAlias.MINUS)
abstract class ModuloOp : BinaryOp(OpAlias.MODULO)
abstract class NeOp : BinaryOp(OpAlias.NE)
abstract class OrOp : BinaryOp(OpAlias.OR)
abstract class PlusOp : BinaryOp(OpAlias.PLUS)
abstract class TimesOp : BinaryOp(OpAlias.TIMES)
abstract class UnionOp : BinaryOp(OpAlias.UNION)
