package org.partiql.lang.typesystem.interfaces.operator.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.SqlOperator
import org.partiql.lang.typesystem.interfaces.type.CompileTimeType
import org.partiql.lang.typesystem.interfaces.type.ScalarType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

sealed class UnaryOp(override val operatorAlias: OpAlias) : SqlOperator {
    /**
     * Type of expression
     */
    abstract val exprType: ScalarType

    /**
     * Function return type inference
     *
     * [typeParametersOfExpr] is type parameters of the operand passed to this operator at compile time.
     */
    abstract fun inferReturnType(typeParametersOfExpr: TypeParameters): List<CompileTimeType>

    /**
     * Evaluation
     *
     * [sourceValue] is the value of the source expression passed to this operator at evaluation time.
     * [typeParametersOfExpr] is type parameters of the operand passed to this operator at compile time.
     */
    abstract fun invoke(sourceValue: ExprValue, typeParametersOfExpr: TypeParameters): ExprValue
}

abstract class NegOp : UnaryOp(OpAlias.NEG)
abstract class NotOp : UnaryOp(OpAlias.NOT)
abstract class PosOp : UnaryOp(OpAlias.POS)
