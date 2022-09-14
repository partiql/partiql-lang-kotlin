package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Failed
import org.partiql.lang.ots_work.interfaces.Successful
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.Uncertain
import org.partiql.lang.ots_work.interfaces.operator.ScalarCastOp
import org.partiql.lang.ots_work.interfaces.type.BoolType
import org.partiql.lang.ots_work.plugins.standard.types.BlobType
import org.partiql.lang.ots_work.plugins.standard.types.CharType
import org.partiql.lang.ots_work.plugins.standard.types.ClobType
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.FloatType
import org.partiql.lang.ots_work.plugins.standard.types.Int2Type
import org.partiql.lang.ots_work.plugins.standard.types.Int4Type
import org.partiql.lang.ots_work.plugins.standard.types.Int8Type
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.types.VarcharType
import org.partiql.lang.ots_work.plugins.standard.types.isLob
import org.partiql.lang.ots_work.plugins.standard.types.isNumeric
import org.partiql.lang.ots_work.plugins.standard.types.isText
import java.time.ZoneOffset

/**
 * @param defaultTimezoneOffset Default timezone offset to be used when TIME WITH TIME ZONE does not explicitly
 */
class StandardScalarCastOp(
    val defaultTimezoneOffset: ZoneOffset,
) : ScalarCastOp() {
    override fun inferType(sourceType: CompileTimeType, targetType: CompileTimeType): TypeInferenceResult {
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
