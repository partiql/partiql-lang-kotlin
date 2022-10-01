package OTS.IMP.org.partiql.ots.legacy.plugin

import OTS.IMP.org.partiql.ots.legacy.operators.LegacyBinaryConcatOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyBinaryDivideOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyBinaryMinusOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyBinaryModuloOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyBinaryPlusOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyBinaryTimesOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyLikeOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyNegOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyNotOp
import OTS.IMP.org.partiql.ots.legacy.operators.LegacyPosOp
import OTS.IMP.org.partiql.ots.legacy.types.BlobType
import OTS.IMP.org.partiql.ots.legacy.types.CharType
import OTS.IMP.org.partiql.ots.legacy.types.ClobType
import OTS.IMP.org.partiql.ots.legacy.types.DecimalType
import OTS.IMP.org.partiql.ots.legacy.types.DecimalTypeParameters
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
import OTS.ITF.org.partiql.ots.operator.ScalarOp
import OTS.ITF.org.partiql.ots.type.BoolType

class LegacyPlugin : Plugin {
    override val binaryPlusOp: ScalarOp = LegacyBinaryPlusOp
    override val binaryMinusOp: ScalarOp = LegacyBinaryMinusOp
    override val binaryTimesOp: ScalarOp = LegacyBinaryTimesOp
    override val binaryDivideOp: ScalarOp = LegacyBinaryDivideOp
    override val binaryModuloOp: ScalarOp = LegacyBinaryModuloOp
    override val posOp: ScalarOp = LegacyPosOp
    override val negOp: ScalarOp = LegacyNegOp
    override val binaryConcatOp: ScalarOp = LegacyBinaryConcatOp
    override val notOp: ScalarOp = LegacyNotOp
    override val likeOp: ScalarOp = LegacyLikeOp

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
                    Int8Type -> {
                        val decimalTypeParameters = DecimalTypeParameters(sourceType.parameters)
                        val precision = decimalTypeParameters.precision
                        val maxDigits = decimalTypeParameters.maxDigits
                        when (precision) {
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

                                if (maxDigits > maxDigitsWithoutPrecisionLoss) {
                                    Uncertain(targetType)
                                } else {
                                    Successful(targetType)
                                }
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
            is DecimalType -> {
                val targetTypeParameters = DecimalTypeParameters(targetType.parameters)
                when (sourceScalarType) {
                    is DecimalType -> {
                        val sourceTypeParameters = DecimalTypeParameters(sourceType.parameters)
                        if (targetTypeParameters.maxDigits >= sourceTypeParameters.maxDigits) {
                            Successful(targetType)
                        } else {
                            Uncertain(targetType)
                        }
                    }
                    is Int2Type,
                    is Int4Type,
                    is Int8Type,
                    is IntType -> when (targetTypeParameters.precision) {
                        null -> Successful(targetType)
                        else -> when (sourceScalarType) {
                            is IntType -> Uncertain(targetType)
                            is Int2Type ->
                                // TODO: Move the magic numbers out
                                // max smallint value 32,767, so the decimal needs to be able to hold at least 5 digits
                                if (targetTypeParameters.maxDigits >= 5) {
                                    Successful(targetType)
                                } else {
                                    Uncertain(targetType)
                                }
                            is Int4Type ->
                                // max int4 value 2,147,483,647 so the decimal needs to be able to hold at least 10 digits
                                if (targetTypeParameters.maxDigits >= 10) {
                                    Successful(targetType)
                                } else {
                                    Uncertain(targetType)
                                }
                            is Int8Type ->
                                // max bigint value 9,223,372,036,854,775,807 so the decimal needs to be able to hold at least 19 digits
                                if (targetTypeParameters.maxDigits >= 19) {
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
