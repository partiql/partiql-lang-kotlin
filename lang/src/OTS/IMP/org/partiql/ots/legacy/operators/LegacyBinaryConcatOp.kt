package OTS.IMP.org.partiql.ots.legacy.operators

import OTS.IMP.org.partiql.ots.legacy.types.CharType
import OTS.IMP.org.partiql.ots.legacy.types.StringType
import OTS.IMP.org.partiql.ots.legacy.types.SymbolType
import OTS.IMP.org.partiql.ots.legacy.types.VarcharType
import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.Failed
import OTS.ITF.org.partiql.ots.Successful
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.operator.ScalarOp
import OTS.ITF.org.partiql.ots.type.ScalarType

object LegacyBinaryConcatOp : ScalarOp {
    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        listOf(StringType.compileTimeType)

    override fun inferReturnType(argsType: List<CompileTimeType>): TypeInferenceResult {
        require(argsType.size == 2) { "Binary Concat operator expects 2 arguments" }

        val lType = argsType[0]
        val rType = argsType[1]

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
