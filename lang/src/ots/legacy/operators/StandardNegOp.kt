package ots.legacy.operators

import ots.CompileTimeType
import ots.TypeInferenceResult
import ots.operator.NegOp
import ots.type.ScalarType

object StandardNegOp : NegOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        StandardPosOp.inferType(argType)
}
