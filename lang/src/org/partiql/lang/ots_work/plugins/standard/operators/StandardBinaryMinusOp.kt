package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.ots_work.interfaces.*
import org.partiql.lang.ots_work.interfaces.operators.BinaryMinusOp

class StandardBinaryMinusOp(
    val valueFactory: ExprValueFactory
) : BinaryMinusOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult =
        inferTypeOfArithmeticOp(lType, rType)

    override fun invoke(lValue: ExprValue, rValue: ExprValue): ExprValue {
        TODO("Not yet implemented")
    }
}