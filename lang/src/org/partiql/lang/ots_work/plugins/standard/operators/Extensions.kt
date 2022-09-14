package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Failed
import org.partiql.lang.ots_work.interfaces.Successful
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.plugins.standard.types.CharType
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.FloatType
import org.partiql.lang.ots_work.plugins.standard.types.Int2Type
import org.partiql.lang.ots_work.plugins.standard.types.Int4Type
import org.partiql.lang.ots_work.plugins.standard.types.Int8Type
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.VarcharType
import org.partiql.lang.ots_work.plugins.standard.types.numberTypesPrecedence

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
