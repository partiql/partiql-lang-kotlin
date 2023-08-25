package org.partiql.cli.utils

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValue.Companion.newBag
import org.partiql.lang.eval.ExprValue.Companion.newBlob
import org.partiql.lang.eval.ExprValue.Companion.newBoolean
import org.partiql.lang.eval.ExprValue.Companion.newClob
import org.partiql.lang.eval.ExprValue.Companion.newDate
import org.partiql.lang.eval.ExprValue.Companion.newDecimal
import org.partiql.lang.eval.ExprValue.Companion.newFloat
import org.partiql.lang.eval.ExprValue.Companion.newInt
import org.partiql.lang.eval.ExprValue.Companion.newList
import org.partiql.lang.eval.ExprValue.Companion.newSexp
import org.partiql.lang.eval.ExprValue.Companion.newString
import org.partiql.lang.eval.ExprValue.Companion.newStruct
import org.partiql.lang.eval.ExprValue.Companion.newSymbol
import org.partiql.lang.eval.ExprValue.Companion.newTime
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.bytesValue
import org.partiql.lang.eval.dateValue
import org.partiql.lang.eval.name
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.timeValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.spi.Plugin
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionParameter
import org.partiql.value.BagValue
import org.partiql.value.BlobValue
import org.partiql.value.BoolValue
import org.partiql.value.CharValue
import org.partiql.value.ClobValue
import org.partiql.value.DateValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue
import org.partiql.value.bagValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.charValue
import org.partiql.value.clobValue
import org.partiql.value.dateValue
import org.partiql.value.datetime.DateTimeValue.date
import org.partiql.value.datetime.DateTimeValue.time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.listValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import org.partiql.value.timeValue
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.URLClassLoader
import java.nio.file.Path
import java.time.DateTimeException
import java.util.ServiceLoader
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A util class to load pluggable functions by invoking Java Service Loader.
 */
class ServiceLoaderUtil {
    @OptIn(PartiQLValueExperimental::class)
    companion object {
        private val lock = ReentrantLock()

        @OptIn(PartiQLFunctionExperimental::class)
        @JvmStatic
        fun loadFunctions(pluginPath: Path): List<ExprFunction> = lock.withLock {
            val pluginsDir = pluginPath.toFile()
            val pluginFolders = pluginsDir.listFiles { file -> file.isDirectory }.orEmpty()
            val files = pluginFolders.flatMap { folder ->
                folder.listFiles { file -> file.isFile && file.extension == "jar" }.orEmpty().toList()
            }
            val plugins = if (files.isNotEmpty()) {
                val classLoader = URLClassLoader.newInstance(files.map { it.toURI().toURL() }.toTypedArray())
                ServiceLoader.load(Plugin::class.java, classLoader)
            } else {
                listOf()
            }
            return plugins.flatMap { plugin -> plugin.getFunctions() }
                .map { partiqlFunc -> PartiQLtoExprFunction(partiqlFunc) }
        }

        @OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
        private fun PartiQLtoExprFunction(customFunction: PartiQLFunction): ExprFunction {
            val name = customFunction.signature.name
            val parameters =
                customFunction.signature.parameters.filterIsInstance<FunctionParameter.ValueParameter>().map { it.type }
            val returnType = customFunction.signature.returns
            return object : ExprFunction {
                override val signature = FunctionSignature(
                    name = name,
                    requiredParameters = parameters.map { PartiQLToStaticType(it) },
                    returnType = PartiQLToStaticType(returnType),
                )

                override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
                    val partiQLArguments = required.mapIndexed { i, expr -> ExprToPartiQLValue(expr, parameters[i]) }
                    val partiQLResult = customFunction.invoke(session.toConnectorSession(), partiQLArguments)
                    return PartiQLtoExprValue(partiQLResult)
                }
            }
        }

        /**
         * All runtime values are nullable in SQL.
         */
        @OptIn(PartiQLValueExperimental::class)
        private fun PartiQLToStaticType(partiqlType: PartiQLValueType): StaticType {
            return when (partiqlType) {
                PartiQLValueType.ANY -> StaticType.ANY.asNullable()
                PartiQLValueType.NULL -> StaticType.NULL
                PartiQLValueType.MISSING -> StaticType.MISSING
                PartiQLValueType.BOOL -> StaticType.BOOL.asNullable()
                PartiQLValueType.INT8 -> StaticType.INT.asNullable()
                PartiQLValueType.INT16 -> StaticType.INT2.asNullable()
                PartiQLValueType.INT32 -> StaticType.INT4.asNullable()
                PartiQLValueType.INT64 -> StaticType.INT8.asNullable()
                PartiQLValueType.INT -> StaticType.INT.asNullable()
                PartiQLValueType.DECIMAL -> StaticType.DECIMAL.asNullable()
                PartiQLValueType.FLOAT32 -> StaticType.FLOAT.asNullable()
                PartiQLValueType.FLOAT64 -> StaticType.FLOAT.asNullable()
                PartiQLValueType.CHAR -> StaticType.STRING.asNullable()
                PartiQLValueType.STRING -> StaticType.STRING.asNullable()
                PartiQLValueType.SYMBOL -> StaticType.SYMBOL.asNullable()
                PartiQLValueType.BINARY -> TODO()
                PartiQLValueType.BYTE -> TODO()
                PartiQLValueType.BLOB -> StaticType.BLOB.asNullable()
                PartiQLValueType.CLOB -> StaticType.CLOB.asNullable()
                PartiQLValueType.DATE -> StaticType.DATE.asNullable()
                PartiQLValueType.TIME -> StaticType.TIME.asNullable()
                PartiQLValueType.TIMESTAMP -> StaticType.TIMESTAMP.asNullable()
                PartiQLValueType.INTERVAL -> TODO()
                PartiQLValueType.BAG -> StaticType.BAG.asNullable()
                PartiQLValueType.LIST -> StaticType.LIST.asNullable()
                PartiQLValueType.SEXP -> StaticType.SEXP.asNullable()
                PartiQLValueType.STRUCT -> StaticType.STRUCT.asNullable()
            }
        }

        @Throws(PartiQLtoExprValueTypeMismatchException::class)
        @OptIn(PartiQLValueExperimental::class)
        private fun PartiQLtoExprValue(partiqlValue: PartiQLValue): ExprValue {
            return when (partiqlValue.type) {
                PartiQLValueType.ANY -> throw UnsupportedOperationException("PartiQLValue ANY not implemented")
                PartiQLValueType.NULL -> ExprValue.nullValue
                PartiQLValueType.MISSING -> ExprValue.missingValue
                PartiQLValueType.BOOL -> (partiqlValue as? BoolValue)?.value?.let { newBoolean(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.INT8 -> (partiqlValue as? Int8Value)?.int?.let { newInt(it) } ?: ExprValue.nullValue

                PartiQLValueType.INT16 -> (partiqlValue as? Int16Value)?.int?.let { newInt(it) } ?: ExprValue.nullValue

                PartiQLValueType.INT32 -> (partiqlValue as? Int32Value)?.int?.let { newInt(it) } ?: ExprValue.nullValue

                PartiQLValueType.INT64 -> (partiqlValue as? Int64Value)?.long?.let { newInt(it) } ?: ExprValue.nullValue

                PartiQLValueType.INT -> (partiqlValue as? IntValue)?.long?.let { newInt(it) } ?: ExprValue.nullValue

                PartiQLValueType.DECIMAL -> (partiqlValue as? DecimalValue)?.value?.let { newDecimal(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.FLOAT32 -> (partiqlValue as? Float32Value)?.double?.let { newFloat(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.FLOAT64 -> (partiqlValue as? Float64Value)?.double?.let { newFloat(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.CHAR -> (partiqlValue as? CharValue)?.string?.let { newString(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.STRING -> (partiqlValue as? StringValue)?.string?.let { newString(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.SYMBOL -> (partiqlValue as? SymbolValue)?.string?.let { newSymbol(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.BINARY -> TODO()

                PartiQLValueType.BYTE -> TODO()

                PartiQLValueType.BLOB -> (partiqlValue as? BlobValue)?.value?.let { newBlob(it) } ?: ExprValue.nullValue

                PartiQLValueType.CLOB -> (partiqlValue as? ClobValue)?.value?.let { newClob(it) } ?: ExprValue.nullValue

                PartiQLValueType.DATE -> (partiqlValue as? DateValue)?.value?.let { newDate(it.year, it.month, it.day) }
                    ?: ExprValue.nullValue

                PartiQLValueType.TIME -> (partiqlValue as? TimeValue)?.value?.let { partiqlTime ->
                    val fraction = partiqlTime.decimalSecond.remainder(BigDecimal.ONE)
                    val precision = when {
                        fraction.scale() > 9 -> throw DateTimeException("Precision greater than nano seconds not supported")
                        else -> fraction.scale()
                    }

                    val tzMinutes = when (val tz = partiqlTime.timeZone) {
                        is TimeZone.UnknownTimeZone -> 0 // Treat unknown offset as UTC (+00:00)
                        is TimeZone.UtcOffset -> tz.totalOffsetMinutes
                        else -> null
                    }

                    try {
                        newTime(
                            Time.of(
                                partiqlTime.hour,
                                partiqlTime.minute,
                                partiqlTime.decimalSecond.setScale(0, RoundingMode.DOWN).toInt(),
                                fraction.movePointRight(9).setScale(0, RoundingMode.DOWN).toInt(),
                                precision,
                                tzMinutes
                            )
                        )
                    } catch (e: DateTimeException) {
                        throw e
                    }
                } ?: ExprValue.nullValue

                PartiQLValueType.TIMESTAMP -> TODO()
                // TODO: Implement
//            {
//                val timestampValue = partiqlValue as? TimestampValue
//                timestampValue?.let { tv ->
//                    val localDateTime = tv.value
//                    val zoneOffset = tv.offset
//                    val instant = localDateTime.atOffset(zoneOffset ?: ZoneOffset.UTC).toInstant()
//                    val timestamp = Timestamp.forMillis(instant.toEpochMilli(), (zoneOffset?.totalSeconds ?: 0) / 60)
//                    newTimestamp(timestamp)
//                } ?: ExprValue.nullValue
//            }

                PartiQLValueType.INTERVAL -> TODO()

                PartiQLValueType.BAG -> {
                    (partiqlValue as? BagValue<*>)?.elements?.map { PartiQLtoExprValue(it) }?.let { newBag(it) }
                        ?: ExprValue.nullValue
                }

                PartiQLValueType.LIST -> {
                    (partiqlValue as? ListValue<*>)?.elements?.map { PartiQLtoExprValue(it) }?.let { newList(it) }
                        ?: ExprValue.nullValue
                }

                PartiQLValueType.SEXP -> {
                    (partiqlValue as? SexpValue<*>)?.elements?.map { PartiQLtoExprValue(it) }?.let { newSexp(it) }
                        ?: ExprValue.nullValue
                }

                PartiQLValueType.STRUCT -> {
                    (partiqlValue as? StructValue<*>)?.fields?.map {
                        PartiQLtoExprValue(it.second).namedValue(
                            newString(
                                it.first
                            )
                        )
                    }?.let { newStruct(it, StructOrdering.ORDERED) } ?: ExprValue.nullValue
                }
            }
        }

        @Throws(ExprToPartiQLValueTypeMismatchException::class)
        @OptIn(PartiQLValueExperimental::class)
        private fun ExprToPartiQLValue(exprValue: ExprValue, partiqlType: PartiQLValueType): PartiQLValue {
            fun checkType(exprType: ExprValueType) {
                if (exprValue.type != exprType) {
                    throw ExprToPartiQLValueTypeMismatchException(partiqlType, ExprToPartiQLValueType(exprValue))
                }
            }

            return when (partiqlType) {
                PartiQLValueType.ANY -> throw UnsupportedOperationException("PartiQLValue ANY not implemented")
                PartiQLValueType.NULL -> {
                    checkType(ExprValueType.NULL)
                    nullValue()
                }
                PartiQLValueType.MISSING -> {
                    checkType(ExprValueType.MISSING)
                    missingValue()
                }
                PartiQLValueType.BOOL -> when (exprValue.type) {
                    ExprValueType.NULL -> boolValue(null)
                    ExprValueType.BOOL -> boolValue(exprValue.booleanValue())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.BOOL, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.INT8 -> when (exprValue.type) {
                    ExprValueType.NULL -> int8Value(null)
                    ExprValueType.INT -> int8Value(exprValue.numberValue().toByte())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.INT8, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.INT16 -> when (exprValue.type) {
                    ExprValueType.NULL -> int16Value(null)
                    ExprValueType.INT -> int16Value(exprValue.numberValue().toShort())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.INT16, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.INT32 -> when (exprValue.type) {
                    ExprValueType.NULL -> int32Value(null)
                    ExprValueType.INT -> int32Value(exprValue.numberValue().toInt())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.INT32, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.INT64 -> when (exprValue.type) {
                    ExprValueType.NULL -> int64Value(null)
                    ExprValueType.INT -> int64Value(exprValue.numberValue().toLong())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.INT64, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.INT -> when (exprValue.type) {
                    ExprValueType.NULL -> intValue(null)
                    ExprValueType.INT -> intValue(exprValue.numberValue() as BigInteger)
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.INT, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.DECIMAL -> when (exprValue.type) {
                    ExprValueType.NULL -> decimalValue(null)
                    ExprValueType.DECIMAL -> decimalValue(exprValue.numberValue() as BigDecimal)
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.DECIMAL, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.FLOAT32 -> when (exprValue.type) {
                    ExprValueType.NULL -> float32Value(null)
                    ExprValueType.FLOAT -> float32Value(exprValue.numberValue().toFloat())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.FLOAT32, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.FLOAT64 -> when (exprValue.type) {
                    ExprValueType.NULL -> float64Value(null)
                    ExprValueType.FLOAT -> float64Value(exprValue.numberValue().toDouble())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.FLOAT64, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.CHAR -> when (exprValue.type) {
                    ExprValueType.NULL -> charValue(null)
                    ExprValueType.STRING -> charValue(exprValue.stringValue().first())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.CHAR, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.STRING -> when (exprValue.type) {
                    ExprValueType.NULL -> stringValue(null)
                    ExprValueType.STRING -> stringValue(exprValue.stringValue())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.STRING, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.SYMBOL -> when (exprValue.type) {
                    ExprValueType.NULL -> symbolValue(null)
                    ExprValueType.SYMBOL -> symbolValue(exprValue.stringValue())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.SYMBOL, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.BINARY -> TODO()
                PartiQLValueType.BYTE -> TODO()
                PartiQLValueType.BLOB -> when (exprValue.type) {
                    ExprValueType.NULL -> blobValue(null)
                    ExprValueType.BLOB -> blobValue(exprValue.bytesValue())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.BLOB, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.CLOB -> when (exprValue.type) {
                    ExprValueType.NULL -> clobValue(null)
                    ExprValueType.CLOB -> clobValue(exprValue.bytesValue())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.CLOB, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.DATE -> when (exprValue.type) {
                    ExprValueType.NULL -> dateValue(null)
                    ExprValueType.DATE -> dateValue(
                        date(
                            exprValue.dateValue().year,
                            exprValue.dateValue().monthValue,
                            exprValue.dateValue().dayOfMonth
                        )
                    )
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.DATE, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.TIME -> when (exprValue.type) {
                    ExprValueType.NULL -> timeValue(null)
                    ExprValueType.TIME -> timeValue(
                        time(
                            exprValue.timeValue().localTime.hour,
                            exprValue.timeValue().localTime.minute,
                            exprValue.timeValue().localTime.second,
                            exprValue.timeValue().localTime.nano,
                            exprValue.timeValue().timezoneMinute?.let { TimeZone.UtcOffset.of(it) } ?: null
                        )
                    )
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.TIME, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.TIMESTAMP -> TODO()
                PartiQLValueType.INTERVAL -> TODO()
                PartiQLValueType.BAG -> when (exprValue.type) {
                    ExprValueType.NULL -> bagValue(null)
                    ExprValueType.BAG -> bagValue(exprValue.map { ExprToPartiQLValue(it, ExprToPartiQLValueType(it)) }.asSequence())
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.BAG, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.LIST -> when (exprValue.type) {
                    ExprValueType.NULL -> listValue(null)
                    ExprValueType.LIST -> listValue(
                        exprValue.map {
                            ExprToPartiQLValue(
                                it, ExprToPartiQLValueType(it)
                            )
                        }.asSequence()
                    )
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.LIST, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.SEXP -> when (exprValue.type) {
                    ExprValueType.NULL -> sexpValue(null)
                    ExprValueType.SEXP -> sexpValue(
                        exprValue.map {
                            ExprToPartiQLValue(
                                it, ExprToPartiQLValueType(it)
                            )
                        }.asSequence()
                    )
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.SEXP, ExprToPartiQLValueType(exprValue)
                    )
                }
                PartiQLValueType.STRUCT -> when (exprValue.type) {
                    ExprValueType.NULL -> structValue(null)
                    ExprValueType.STRUCT -> structValue(
                        exprValue.map {
                            Pair(
                                it.name?.stringValue() ?: "", ExprToPartiQLValue(it, ExprToPartiQLValueType(it))
                            )
                        }.asSequence()
                    )
                    else -> throw ExprToPartiQLValueTypeMismatchException(
                        PartiQLValueType.STRUCT, ExprToPartiQLValueType(exprValue)
                    )
                }
            }
        }

        fun ExprToPartiQLValueType(exprValue: ExprValue): PartiQLValueType {
            return when (exprValue.type) {
                ExprValueType.MISSING -> PartiQLValueType.MISSING
                ExprValueType.NULL -> PartiQLValueType.NULL
                ExprValueType.BOOL -> PartiQLValueType.BOOL
                ExprValueType.INT -> PartiQLValueType.INT
                ExprValueType.FLOAT -> PartiQLValueType.FLOAT32
                ExprValueType.DECIMAL -> PartiQLValueType.DECIMAL
                ExprValueType.DATE -> PartiQLValueType.DATE
                ExprValueType.TIMESTAMP -> PartiQLValueType.TIMESTAMP
                ExprValueType.TIME -> PartiQLValueType.TIME
                ExprValueType.SYMBOL -> PartiQLValueType.SYMBOL
                ExprValueType.STRING -> PartiQLValueType.STRING
                ExprValueType.CLOB -> PartiQLValueType.CLOB
                ExprValueType.BLOB -> PartiQLValueType.BLOB
                ExprValueType.LIST -> PartiQLValueType.LIST
                ExprValueType.SEXP -> PartiQLValueType.SEXP
                ExprValueType.STRUCT -> PartiQLValueType.STRUCT
                ExprValueType.BAG -> PartiQLValueType.BAG
                ExprValueType.GRAPH -> TODO()
            }
        }
    }
}
