package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

abstract class UnaryOp internal constructor() : PqlOperator {
    /**
     * Type of expression
     */
    abstract val exprType: SqlType

    /**
     * Function return type inference
     *
     * [typeParametersOfExpr] is type parameters of the operand passed to this operator at compile time.
     */
    abstract fun inferReturnType(typeParametersOfExpr: TypeParameters): List<TypeWithParameters>

    /**
     * Evaluation
     *
     * [sourceValue] is the value of the source expression passed to this operator at evaluation time.
     * [typeParametersOfExpr] is type parameters of the operand passed to this operator at compile time.
     */
    abstract fun invoke(sourceValue: ExprValue, typeParametersOfExpr: TypeParameters): ExprValue
}
