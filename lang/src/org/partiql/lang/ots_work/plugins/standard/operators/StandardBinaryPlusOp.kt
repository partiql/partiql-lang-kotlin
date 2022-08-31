package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.operator.BinaryPlusOp
import org.partiql.lang.ots_work.interfaces.type.ScalarType
import org.partiql.lang.util.plus

class StandardBinaryPlusOp(
    val valueFactory: ExprValueFactory
) : BinaryPlusOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult =
        inferTypeOfArithmeticOp(lType, rType)

    override fun invoke(lValue: ExprValue, rValue: ExprValue): ExprValue =
        (lValue.numberValue() + rValue.numberValue()).exprValue(valueFactory)
}
