package ots.legacy.operators

import ots.CompileTimeType
import ots.TypeInferenceResult
import ots.operator.BinaryPlusOp
import ots.type.ScalarType

object StandardBinaryPlusOp : BinaryPlusOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult =
        inferTypeOfArithmeticOp(lType, rType)
}
