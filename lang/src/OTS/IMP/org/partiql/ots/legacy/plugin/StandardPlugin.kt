package OTS.IMP.org.partiql.ots.legacy.plugin

import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryConcatOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryDivideOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryMinusOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryModuloOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryPlusOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryTimesOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardLikeOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardNegOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardNotOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardPosOp
import OTS.IMP.org.partiql.ots.legacy.types.BlobType
import OTS.IMP.org.partiql.ots.legacy.types.CharType
import OTS.IMP.org.partiql.ots.legacy.types.ClobType
import OTS.IMP.org.partiql.ots.legacy.types.DecimalType
import OTS.IMP.org.partiql.ots.legacy.types.FloatType
import OTS.IMP.org.partiql.ots.legacy.types.Int2Type
import OTS.IMP.org.partiql.ots.legacy.types.Int4Type
import OTS.IMP.org.partiql.ots.legacy.types.Int8Type
import OTS.IMP.org.partiql.ots.legacy.types.IntType
import OTS.IMP.org.partiql.ots.legacy.types.StringType
import OTS.IMP.org.partiql.ots.legacy.types.SymbolType
import OTS.IMP.org.partiql.ots.legacy.types.TimeStampType
import OTS.IMP.org.partiql.ots.legacy.types.VarcharType
import OTS.IMP.org.partiql.ots.legacy.types.isLob
import OTS.IMP.org.partiql.ots.legacy.types.isNumeric
import OTS.IMP.org.partiql.ots.legacy.types.isText
import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.Failed
import OTS.ITF.org.partiql.ots.Plugin
import OTS.ITF.org.partiql.ots.Successful
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.Uncertain
import OTS.ITF.org.partiql.ots.operator.BinaryConcatOp
import OTS.ITF.org.partiql.ots.operator.BinaryDivideOp
import OTS.ITF.org.partiql.ots.operator.BinaryMinusOp
import OTS.ITF.org.partiql.ots.operator.BinaryModuloOp
import OTS.ITF.org.partiql.ots.operator.BinaryPlusOp
import OTS.ITF.org.partiql.ots.operator.BinaryTimesOp
import OTS.ITF.org.partiql.ots.operator.LikeOp
import OTS.ITF.org.partiql.ots.operator.NegOp
import OTS.ITF.org.partiql.ots.operator.NotOp
import OTS.ITF.org.partiql.ots.operator.PosOp
import OTS.ITF.org.partiql.ots.type.BoolType
import com.amazon.ion.Timestamp
import java.time.ZoneOffset

data class StandardPlugin(
    val defaultTimezoneOffset: ZoneOffset = ZoneOffset.UTC,
    val now: Timestamp = Timestamp.nowZ()
) : Plugin {
    override val binaryPlusOp: BinaryPlusOp = StandardBinaryPlusOp
    override val binaryMinusOp: BinaryMinusOp = StandardBinaryMinusOp
    override val binaryTimesOp: BinaryTimesOp = StandardBinaryTimesOp
    override val binaryDivideOp: BinaryDivideOp = StandardBinaryDivideOp
    override val binaryModuloOp: BinaryModuloOp = StandardBinaryModuloOp
    override val posOp: PosOp = StandardPosOp
    override val negOp: NegOp = StandardNegOp
    override val binaryConcatOp: BinaryConcatOp = StandardBinaryConcatOp
    override val notOp: NotOp = StandardNotOp
    override val likeOp: LikeOp = StandardLikeOp

    override fun scalarTypeCastInference(sourceType: CompileTimeType, targetType: CompileTimeType): TypeInferenceResult {
        val targetScalarType = targetType.scalarType
        val sourceScalarType = sourceType.scalarType

        return when (targetScalarType) {
            is VarcharType,
            is CharType,
            is StringType,
            is SymbolType -> when {
                sourceScalarType.isNumeric() || sourceScalarType.isText() -> Successful(targetType)
                sourceScalarType in listOf(BoolType, TimeStampType) -> Successful(targetType)
                else -> Failed
            }
            is Int2Type,
            is Int4Type,
            is Int8Type,
            is IntType -> when (sourceScalarType) {
                is Int2Type,
                is Int4Type,
                is Int8Type,
                is IntType -> {
                    when (targetScalarType) {
                        is Int2Type -> when (sourceScalarType) {
                            is Int2Type -> Successful(targetType)
                            is Int4Type,
                            is Int8Type,
                            is IntType -> Uncertain(targetType)
                            else -> error("Unreachable code")
                        }
                        is Int4Type -> when (sourceScalarType) {
                            is Int2Type,
                            is Int4Type -> Successful(targetType)
                            is Int8Type,
                            is IntType -> Uncertain(targetType)
                            else -> error("Unreachable code")
                        }
                        is Int8Type -> when (sourceScalarType) {
                            is Int2Type,
                            is Int4Type,
                            is Int8Type -> Successful(targetType)
                            is IntType -> Uncertain(targetType)
                            else -> error("Unreachable code")
                        }
                        is IntType -> Successful(targetType)
                        else -> error("Unreachable code")
                    }
                }
                is BoolType -> Successful(targetType)
                is FloatType -> when (targetScalarType) {
                    IntType -> Successful(targetType)
                    else -> Uncertain(targetType)
                }
                is DecimalType -> when (targetScalarType) {
                    IntType -> Successful(targetType)
                    Int2Type,
                    Int4Type,
                    Int8Type -> when (sourceType.parameters[0]) {
                        null -> Uncertain(targetType)
                        else -> {
                            // Max value of SMALLINT is 32767.
                            // Conversion to SMALLINT will work as long as the decimal holds up 4 to digits. There is a chance of overflow beyond that.
                            // Similarly -
                            //   Max value of INT4 is 2,147,483,647
                            //   Max value of BIGINT is 9,223,372,036,854,775,807 for BIGINT
                            // TODO: Move these magic numbers out.
                            val maxDigitsWithoutPrecisionLoss = when (targetScalarType) {
                                Int2Type -> 4
                                Int4Type -> 9
                                Int8Type -> 18
                                IntType -> error("Un-constrained is handled above. This code shouldn't be reached.")
                                else -> error("Unreachable code")
                            }

                            if (sourceScalarType.maxDigits(sourceType.parameters) > maxDigitsWithoutPrecisionLoss) {
                                Uncertain(targetType)
                            } else {
                                Successful(targetType)
                            }
                        }
                    }
                    else -> error("Unreachable code")
                }
                is SymbolType,
                is StringType,
                is CharType,
                is VarcharType -> Uncertain(targetType)
                else -> Failed
            }
            is BoolType -> when {
                sourceScalarType === BoolType || sourceScalarType.isNumeric() || sourceScalarType.isText() -> Successful(targetType)
                else -> Failed
            }
            is FloatType -> when {
                sourceScalarType === BoolType -> Successful(targetType)
                // Conversion to float will always succeed for numeric types
                sourceScalarType.isNumeric() -> Successful(targetType)
                sourceScalarType.isText() -> Uncertain(targetType)
                else -> Failed
            }
            is DecimalType -> when (sourceScalarType) {
                is DecimalType ->
                    if (targetScalarType.maxDigits(targetType.parameters) >= sourceScalarType.maxDigits(sourceType.parameters)) {
                        Successful(targetType)
                    } else {
                        Uncertain(targetType)
                    }
                is Int2Type,
                is Int4Type,
                is Int8Type,
                is IntType -> when (targetType.parameters[0]) {
                    null -> Successful(targetType)
                    else -> when (sourceScalarType) {
                        is IntType -> Uncertain(targetType)
                        is Int2Type ->
                            // TODO: Move the magic numbers out
                            // max smallint value 32,767, so the decimal needs to be able to hold at least 5 digits
                            if (targetScalarType.maxDigits(targetType.parameters) >= 5) {
                                Successful(targetType)
                            } else {
                                Uncertain(targetType)
                            }
                        is Int4Type ->
                            // max int4 value 2,147,483,647 so the decimal needs to be able to hold at least 10 digits
                            if (targetScalarType.maxDigits(targetType.parameters) >= 10) {
                                Successful(targetType)
                            } else {
                                Uncertain(targetType)
                            }
                        is Int8Type ->
                            // max bigint value 9,223,372,036,854,775,807 so the decimal needs to be able to hold at least 19 digits
                            if (targetScalarType.maxDigits(targetType.parameters) >= 19) {
                                Successful(targetType)
                            } else {
                                Uncertain(targetType)
                            }
                        else -> error("Unreachable code")
                    }
                }
                is BoolType,
                is FloatType -> Successful(targetType)
                is SymbolType,
                is StringType,
                is CharType,
                is VarcharType -> Uncertain(targetType)
                else -> Failed
            }
            is ClobType,
            is BlobType -> when {
                sourceScalarType.isLob() -> Successful(targetType)
                else -> Failed
            }
            is TimeStampType -> when {
                sourceScalarType === TimeStampType -> Successful(targetType)
                sourceScalarType.isText() -> Uncertain(targetType)
                else -> Failed
            }
            else -> Failed
        }
    }
}
