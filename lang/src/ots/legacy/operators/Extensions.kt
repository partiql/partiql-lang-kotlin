package ots.legacy.operators

import ots.CompileTimeType
import ots.Failed
import ots.Successful
import ots.TypeInferenceResult
import ots.legacy.types.CharType
import ots.legacy.types.DecimalType
import ots.legacy.types.FloatType
import ots.legacy.types.Int2Type
import ots.legacy.types.Int4Type
import ots.legacy.types.Int8Type
import ots.legacy.types.IntType
import ots.legacy.types.StringType
import ots.legacy.types.SymbolType
import ots.legacy.types.VarcharType
import ots.legacy.types.numberTypesPrecedence

internal val ALL_TEXT_TYPES = listOf(SymbolType, StringType, CharType, VarcharType)
internal val ALL_NUMBER_TYPES = listOf(Int2Type, Int4Type, Int8Type, IntType, FloatType, DecimalType)

internal val defaultReturnTypesOfArithmeticOp = listOf(
    Int2Type.compileTimeType,
    Int4Type.compileTimeType,
    Int8Type.compileTimeType,
    IntType.compileTimeType,
    FloatType.compileTimeType,
    DecimalType.compileTimeType
)

internal fun inferTypeOfArithmeticOp(lhs: CompileTimeType, rhs: CompileTimeType): TypeInferenceResult {
    val leftType = lhs.scalarType
    val rightType = rhs.scalarType
    if (leftType !in ALL_NUMBER_TYPES || rightType !in ALL_NUMBER_TYPES) {
        return Failed
    }
    if (leftType === DecimalType || rightType === DecimalType) {
        return Successful(DecimalType.compileTimeType) // TODO:  account for decimal precision
    }

    val leftPrecedence = numberTypesPrecedence.indexOf(leftType)
    val rightPrecedence = numberTypesPrecedence.indexOf(rightType)

    return when {
        leftPrecedence > rightPrecedence -> Successful(lhs)
        else -> Successful(rhs)
    }
}
