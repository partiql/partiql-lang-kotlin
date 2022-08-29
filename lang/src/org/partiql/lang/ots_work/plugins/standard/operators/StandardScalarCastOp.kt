package org.partiql.lang.ots_work.plugins.standard.operators

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import com.amazon.ion.Timestamp
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.bytesValue
import org.partiql.lang.eval.dateValue
import org.partiql.lang.eval.errIntOverflow
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.time.genericTimeRegex
import org.partiql.lang.eval.time.getPrecisionFromTimeString
import org.partiql.lang.eval.timeValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.ots_work.interfaces.BoolType
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Failed
import org.partiql.lang.ots_work.interfaces.Successful
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.Uncertain
import org.partiql.lang.ots_work.interfaces.operators.ScalarCastOp
import org.partiql.lang.ots_work.plugins.standard.plugin.TypedOpBehavior
import org.partiql.lang.ots_work.plugins.standard.types.BlobType
import org.partiql.lang.ots_work.plugins.standard.types.CharType
import org.partiql.lang.ots_work.plugins.standard.types.ClobType
import org.partiql.lang.ots_work.plugins.standard.types.DateType
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.FloatType
import org.partiql.lang.ots_work.plugins.standard.types.Int2Type
import org.partiql.lang.ots_work.plugins.standard.types.Int4Type
import org.partiql.lang.ots_work.plugins.standard.types.Int8Type
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.types.TimeType
import org.partiql.lang.ots_work.plugins.standard.types.VarcharType
import org.partiql.lang.ots_work.plugins.standard.types.isLob
import org.partiql.lang.ots_work.plugins.standard.types.isNumeric
import org.partiql.lang.ots_work.plugins.standard.types.isText
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.coerce
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.ionValue
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.round

/**
 * @param defaultTimezoneOffset Default timezone offset to be used when TIME WITH TIME ZONE does not explicitly
 */
class StandardScalarCastOp(
    val typedOpBehavior: TypedOpBehavior,
    val valueFactory: ExprValueFactory,
    val defaultTimezoneOffset: ZoneOffset,
    // TODO: remove the following field and move location-meta-related error handling to scalar type system core
    var currentLocationMeta: SourceLocationMeta? = null
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

    override fun invoke(sourceValue: ExprValue, targetType: CompileTimeType): ExprValue? {
        val sourceType = sourceValue.type

        if (sourceType == targetType.scalarType.runTimeType && sourceType != ExprValueType.TIME) {
            when (targetType.scalarType) {
                in listOf(Int2Type, Int4Type, Int8Type, IntType, FloatType, DecimalType) -> return sourceValue.numberValue().exprValue(sourceValue, targetType)
                in listOf(CharType, VarcharType, StringType) -> return sourceValue.stringValue().exprValue(targetType)
                else -> return sourceValue // Blob, Bool,Clob, Date, Symbol, Timestamp
            }
        }

        when (val targetScalarType = targetType.scalarType) {
            is CharType,
            is VarcharType,
            is StringType,
            is SymbolType -> when {
                sourceType.isNumber -> return sourceValue.numberValue().toString().exprValue(targetType)
                sourceType.isText -> return sourceValue.stringValue().exprValue(targetType)
                sourceType == ExprValueType.DATE -> return sourceValue.dateValue().toString().exprValue(targetType)
                sourceType == ExprValueType.TIME -> return sourceValue.timeValue().toString().exprValue(targetType)
                sourceType in ION_TEXT_STRING_CAST_TYPES -> return sourceValue.ionValue.toString().exprValue(targetType)
            }
            is Int2Type,
            is Int4Type,
            is Int8Type,
            is IntType -> when {
                sourceType == ExprValueType.BOOL -> return if (sourceValue.booleanValue()) 1L.exprValue(sourceValue, targetType) else 0L.exprValue(sourceValue, targetType)
                sourceType.isNumber -> return sourceValue.numberValue().exprValue(sourceValue, targetType)
                sourceType.isText -> {
                    val value = try {
                        val normalized = sourceValue.stringValue().normalizeForCastToInt()
                        valueFactory.ion.singleValue(normalized) as IonInt
                    } catch (e: Exception) {
                        castFailedErr(sourceValue, "can't convert string value to INT", false, e, targetScalarType, currentLocationMeta)
                    }

                    return when (value.integerSize) {
                        // Our numbers comparison machinery does not handle big integers yet, fail fast
                        IntegerSize.BIG_INTEGER -> errIntOverflow(8, errorContextFrom(currentLocationMeta))
                        else -> value.longValue().exprValue(sourceValue, targetType)
                    }
                }
            }
            is FloatType -> when {
                sourceType == ExprValueType.BOOL -> return if (sourceValue.booleanValue()) 1.0.exprValue(sourceValue, targetType) else 0.0.exprValue(sourceValue, targetType)
                sourceType.isNumber -> return sourceValue.numberValue().toDouble().exprValue(sourceValue, targetType)
                sourceType.isText ->
                    try {
                        return sourceValue.stringValue().toDouble().exprValue(sourceValue, targetType)
                    } catch (e: NumberFormatException) {
                        castFailedErr(sourceValue, "can't convert string value to FLOAT", false, e, targetScalarType, currentLocationMeta)
                    }
            }
            is DecimalType -> when {
                sourceType == ExprValueType.BOOL -> return if (sourceValue.booleanValue()) {
                    BigDecimal.ONE.exprValue(sourceValue, targetType)
                } else {
                    BigDecimal.ZERO.exprValue(sourceValue, targetType)
                }
                sourceType.isNumber -> return sourceValue.numberValue().exprValue(sourceValue, targetType)
                sourceType.isText -> try {
                    return bigDecimalOf(sourceValue.stringValue()).exprValue(sourceValue, targetType)
                } catch (e: NumberFormatException) {
                    castFailedErr(sourceValue, "can't convert string value to DECIMAL", false, e, targetScalarType, currentLocationMeta)
                }
            }
            is BlobType -> when {
                sourceType.isLob -> return valueFactory.newBlob(sourceValue.bytesValue())
            }
            is ClobType -> when {
                sourceType.isLob -> return valueFactory.newClob(sourceValue.bytesValue())
            }
            is BoolType -> when {
                sourceType.isNumber -> return when {
                    sourceValue.numberValue().compareTo(0L) == 0 -> valueFactory.newBoolean(false)
                    else -> valueFactory.newBoolean(true)
                }
                sourceType.isText -> return when (sourceValue.stringValue().toLowerCase()) {
                    "true" -> valueFactory.newBoolean(true)
                    "false" -> valueFactory.newBoolean(false)
                    else -> castFailedErr(sourceValue, "can't convert string value to BOOL", internal = false, targetScalarType = targetScalarType, locationMeta = currentLocationMeta)
                }
            }
            is TimeStampType -> when {
                sourceType.isText -> try {
                    return valueFactory.newTimestamp(Timestamp.valueOf(sourceValue.stringValue()))
                } catch (e: IllegalArgumentException) {
                    castFailedErr(sourceValue, "can't convert string value to TIMESTAMP", internal = false, cause = e, targetScalarType = targetScalarType, locationMeta = currentLocationMeta)
                }
            }
            is DateType -> when {
                sourceType == ExprValueType.TIMESTAMP -> {
                    val ts = sourceValue.timestampValue()
                    return valueFactory.newDate(LocalDate.of(ts.year, ts.month, ts.day))
                }
                sourceType.isText -> try {
                    // validate that the date string follows the format YYYY-MM-DD
                    if (!datePatternRegex.matches(sourceValue.stringValue())) {
                        castFailedErr(
                            sourceValue,
                            "Can't convert string value to DATE. Expected valid date string " +
                                "and the date format to be YYYY-MM-DD",
                            internal = false,
                            targetScalarType = targetScalarType,
                            locationMeta = currentLocationMeta
                        )
                    }
                    val date = LocalDate.parse(sourceValue.stringValue())
                    return valueFactory.newDate(date)
                } catch (e: DateTimeParseException) {
                    castFailedErr(
                        sourceValue,
                        "Can't convert string value to DATE. Expected valid date string " +
                            "and the date format to be YYYY-MM-DD",
                        internal = false,
                        cause = e,
                        targetScalarType = targetScalarType,
                        locationMeta = currentLocationMeta
                    )
                }
            }
            is TimeType -> {
                val precision = targetType.parameters[0]
                when {
                    sourceType == ExprValueType.TIME -> {
                        val time = sourceValue.timeValue()
                        val timeZoneOffset = when (targetScalarType.withTimeZone) {
                            true -> time.zoneOffset ?: defaultTimezoneOffset
                            else -> null
                        }
                        return valueFactory.newTime(
                            Time.of(
                                time.localTime,
                                precision ?: time.precision,
                                timeZoneOffset
                            )
                        )
                    }
                    sourceType == ExprValueType.TIMESTAMP -> {
                        val ts = sourceValue.timestampValue()
                        val timeZoneOffset = when (targetScalarType.withTimeZone) {
                            true -> ts.localOffset ?: castFailedErr(
                                sourceValue,
                                "Can't convert timestamp value with unknown local offset (i.e. -00:00) to TIME WITH TIME ZONE.",
                                internal = false,
                                targetScalarType = targetScalarType,
                                locationMeta = currentLocationMeta
                            )
                            else -> null
                        }
                        return valueFactory.newTime(
                            Time.of(
                                ts.hour,
                                ts.minute,
                                ts.second,
                                (ts.decimalSecond.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal())).toInt(),
                                precision ?: ts.decimalSecond.scale(),
                                timeZoneOffset
                            )
                        )
                    }
                    sourceType.isText -> try {
                        // validate that the time string follows the format HH:MM:SS[.ddddd...][+|-HH:MM]
                        val matcher = genericTimeRegex.toPattern().matcher(sourceValue.stringValue())
                        if (!matcher.find()) {
                            castFailedErr(
                                sourceValue,
                                "Can't convert string value to TIME. Expected valid time string " +
                                    "and the time to be of the format HH:MM:SS[.ddddd...][+|-HH:MM]",
                                internal = false,
                                targetScalarType = targetScalarType,
                                locationMeta = currentLocationMeta
                            )
                        }

                        val localTime = LocalTime.parse(sourceValue.stringValue(), DateTimeFormatter.ISO_TIME)

                        // Note that the [genericTimeRegex] has a group to extract the zone offset.
                        val zoneOffsetString = matcher.group(2)
                        val zoneOffset = zoneOffsetString?.let { ZoneOffset.of(it) } ?: defaultTimezoneOffset

                        return valueFactory.newTime(
                            Time.of(
                                localTime,
                                precision ?: getPrecisionFromTimeString(sourceValue.stringValue()),
                                when (targetScalarType.withTimeZone) {
                                    true -> zoneOffset
                                    else -> null
                                }
                            )
                        )
                    } catch (e: DateTimeParseException) {
                        castFailedErr(
                            sourceValue,
                            "Can't convert string value to TIME. Expected valid time string " +
                                "and the time format to be HH:MM:SS[.ddddd...][+|-HH:MM]",
                            internal = false,
                            cause = e,
                            targetScalarType = targetScalarType,
                            locationMeta = currentLocationMeta
                        )
                    }
                }
            }
            else -> error("Unsupported type: $targetScalarType")
        }

        return null
    }

    private fun Number.exprValue(sourceValue: ExprValue, targetType: CompileTimeType) = when (val targetScalarType = targetType.scalarType) {
        is Int2Type,
        is Int4Type,
        is Int8Type,
        is IntType -> {
            val rangeForType = when (typedOpBehavior) {
                // Legacy behavior doesn't honor SMALLINT, INT4 constraints
                TypedOpBehavior.LEGACY -> LongRange(Long.MIN_VALUE, Long.MAX_VALUE)
                TypedOpBehavior.HONOR_PARAMETERS ->
                    when (targetType.scalarType) {
                        // There is not CAST syntax to that can execute this branch today.
                        is Int2Type -> LongRange(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong())
                        is Int4Type -> LongRange(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                        is Int8Type,
                        is IntType -> LongRange(Long.MIN_VALUE, Long.MAX_VALUE)
                        else -> error("Unreachable code")
                    }
            }

            // Here, we check if there is a possibility of being able to fit this number into
            // any of the integer types. We allow the buffer of 1 because we allow rounding into min/max values.
            if (this <= (longMinDecimal - BigDecimal.ONE) || this >= (longMaxDecimal + BigDecimal.ONE)) {
                errIntOverflow(8)
            }

            // We round the value to the nearest integral value
            // In legacy behavior, this always picks the floor integer value
            // Else, rounding is done through https://en.wikipedia.org/wiki/Rounding#Round_half_to_even
            // We don't convert the result to Long within the when block here
            //  because the rounded values can still be out of range for Kotlin's Long.
            val result = when (typedOpBehavior) {
                TypedOpBehavior.LEGACY -> when (this) {
                    // BigDecimal.toLong inflates the internal BigInteger to the scale before converting it to a long.
                    // For example to convert 1e-6000 it needs to create a BigInteger with value equal to
                    // `unscaledNumber^(10^abs(scale))` to them drop it and return 0L. The BigInteger creation is very
                    // expensive and completely wasted. The division to integral skips all that.
                    is BigDecimal -> this.divideToIntegralValue(BigDecimal.ONE)
                    else -> this
                }
                TypedOpBehavior.HONOR_PARAMETERS -> when (this) {
                    is BigDecimal -> this.setScale(0, RoundingMode.HALF_EVEN)
                    // [kotlin.math.round] rounds towards the closes even number on tie
                    //   https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.math/round.html
                    is Float -> round(this)
                    is Double -> round(this)
                    else -> this
                }
            }.let {
                // after rounding, check that the value can fit into range of the type being casted into
                if (it < rangeForType.first || it > rangeForType.last) {
                    errIntOverflow(8)
                }
                it.toLong()
            }
            valueFactory.newInt(result)
        }
        is FloatType -> valueFactory.newFloat(this.toDouble())
        is DecimalType -> when (typedOpBehavior) {
            TypedOpBehavior.LEGACY -> valueFactory.newFromIonValue(
                this.coerce(BigDecimal::class.java).ionValue(valueFactory.ion)
            )
            TypedOpBehavior.HONOR_PARAMETERS -> when (val precision = targetType.parameters[0]) {
                null -> valueFactory.newFromIonValue(
                    this.coerce(BigDecimal::class.java).ionValue(valueFactory.ion)
                )
                else -> {
                    val decimal = this.coerce(BigDecimal::class.java) as BigDecimal
                    val scale = targetType.parameters[1]!!
                    val result = decimal.round(MathContext(precision))
                        .setScale(scale, RoundingMode.HALF_UP)
                    if (result.precision() > precision) {
                        // Following PostgresSQL behavior here. Java will increase precision if needed.
                        castFailedErr(
                            sourceValue,
                            "target type DECIMAL($precision, $scale) too small for value $decimal.",
                            internal = false,
                            targetScalarType = targetScalarType,
                            locationMeta = currentLocationMeta
                        )
                    } else {
                        valueFactory.newFromIonValue(result.ionValue(valueFactory.ion))
                    }
                }
            }
        }
        else -> error("Unreachable code")
    }

    private fun String.exprValue(targetType: CompileTimeType) = when (val targetScalarType = targetType.scalarType) {
        is CharType,
        is VarcharType,
        is StringType -> when (typedOpBehavior) {
            TypedOpBehavior.LEGACY -> valueFactory.newString(this)
            TypedOpBehavior.HONOR_PARAMETERS -> when (targetScalarType) {
                is StringType -> valueFactory.newString(this)
                is CharType,
                is VarcharType -> {
                    val actualCodepointCount = this.codePointCount(0, this.length)
                    val lengthConstraint = targetType.parameters[0]!!
                    val truncatedString = if (actualCodepointCount <= lengthConstraint) {
                        this // no truncation needed
                    } else {
                        this.substring(0, this.offsetByCodePoints(0, lengthConstraint))
                    }

                    valueFactory.newString(
                        when (targetScalarType) {
                            is CharType -> truncatedString.trimEnd { c -> c == '\u0020' }
                            is VarcharType -> truncatedString
                            else -> error("Unreachable code")
                        }
                    )
                }
                else -> error("Unreachable code")
            }
        }
        is SymbolType -> valueFactory.newSymbol(this)
        else -> error("Unreachable code")
    }
}
