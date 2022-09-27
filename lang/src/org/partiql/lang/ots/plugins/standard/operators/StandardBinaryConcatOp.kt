package org.partiql.lang.ots.plugins.standard.operators

import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.Failed
import org.partiql.lang.ots.interfaces.Successful
import org.partiql.lang.ots.interfaces.TypeInferenceResult
import org.partiql.lang.ots.interfaces.operator.BinaryConcatOp
import org.partiql.lang.ots.interfaces.type.ScalarType
import org.partiql.lang.ots.plugins.standard.types.CharType
import org.partiql.lang.ots.plugins.standard.types.StringType
import org.partiql.lang.ots.plugins.standard.types.SymbolType
import org.partiql.lang.ots.plugins.standard.types.VarcharType

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
