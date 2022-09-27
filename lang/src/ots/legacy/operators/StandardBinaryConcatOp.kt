package ots.legacy.operators

import ots.CompileTimeType
import ots.Failed
import ots.Successful
import ots.TypeInferenceResult
import ots.legacy.types.CharType
import ots.legacy.types.StringType
import ots.legacy.types.SymbolType
import ots.legacy.types.VarcharType
import ots.operator.BinaryConcatOp
import ots.type.ScalarType

object StandardBinaryConcatOp : BinaryConcatOp() {
    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        listOf(StringType.compileTimeType)

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult {
        val leftType = lType.scalarType
        val rightType = rType.scalarType
        if (leftType !in validOperandTypes || rightType !in validOperandTypes) {
            return Failed
        }
        if (leftType === SymbolType || leftType === StringType || rightType === SymbolType || rightType === StringType) {
            return Successful(StringType.compileTimeType)
        }

        // Here only VARCHAR or CHAR exists
        val leftLength = lType.parameters[0]!!
        val rightLength = rType.parameters[0]!!
        val sumLength = leftLength + rightLength
        val returnType = when {
            leftType === CharType && rightType === CharType -> CharType
            else -> VarcharType
        }

        return Successful(
            CompileTimeType(
                scalarType = returnType,
                parameters = listOf(sumLength)
            )
        )
    }
}
