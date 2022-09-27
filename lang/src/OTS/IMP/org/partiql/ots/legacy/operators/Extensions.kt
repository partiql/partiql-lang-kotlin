package OTS.IMP.org.partiql.ots.legacy.operators

import OTS.IMP.org.partiql.ots.legacy.types.CharType
import OTS.IMP.org.partiql.ots.legacy.types.DecimalType
import OTS.IMP.org.partiql.ots.legacy.types.FloatType
import OTS.IMP.org.partiql.ots.legacy.types.Int2Type
import OTS.IMP.org.partiql.ots.legacy.types.Int4Type
import OTS.IMP.org.partiql.ots.legacy.types.Int8Type
import OTS.IMP.org.partiql.ots.legacy.types.IntType
import OTS.IMP.org.partiql.ots.legacy.types.StringType
import OTS.IMP.org.partiql.ots.legacy.types.SymbolType
import OTS.IMP.org.partiql.ots.legacy.types.VarcharType
import OTS.IMP.org.partiql.ots.legacy.types.numberTypesPrecedence
import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.Failed
import OTS.ITF.org.partiql.ots.Successful
import OTS.ITF.org.partiql.ots.TypeInferenceResult

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
