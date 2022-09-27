package ots.legacy.operators

import ots.CompileTimeType
import ots.Failed
import ots.Successful
import ots.TypeInferenceResult
import ots.operator.PosOp
import ots.type.ScalarType

object StandardPosOp : PosOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        when (argType.scalarType) {
            in validOperandTypes -> Successful(argType)
            else -> Failed
        }
}
