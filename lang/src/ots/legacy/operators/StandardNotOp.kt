package ots.legacy.operators

import ots.CompileTimeType
import ots.Failed
import ots.Successful
import ots.TypeInferenceResult
import ots.operator.NotOp
import ots.type.BoolType
import ots.type.ScalarType

object StandardNotOp : NotOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        listOf(BoolType.compileTimeType)

    override val validOperandTypes: List<ScalarType> =
        listOf(BoolType)

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        when (argType.scalarType) {
            BoolType -> Successful(argType)
            else -> Failed
        }
}
