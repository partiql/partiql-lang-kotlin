package ots.legacy.operators

import ots.CompileTimeType
import ots.Failed
import ots.Successful
import ots.TypeInferenceResult
import ots.Uncertain
import ots.operator.LikeOp
import ots.type.BoolType
import ots.type.ScalarType

object StandardLikeOp : LikeOp() {
    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override fun inferType(value: CompileTimeType, pattern: CompileTimeType, escape: CompileTimeType?): TypeInferenceResult =
        when {
            value.scalarType !in validOperandTypes || pattern.scalarType !in validOperandTypes -> Failed
            escape === null -> Successful(BoolType.compileTimeType)
            escape.scalarType in validOperandTypes -> Uncertain(BoolType.compileTimeType)
            else -> Failed
        }
}
