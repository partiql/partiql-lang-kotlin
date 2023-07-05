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
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.PartiQLFunction
import org.partiql.types.PartiQLValueType
import org.partiql.types.StaticType
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
import org.partiql.value.NullableBagValue
import org.partiql.value.NullableBlobValue
import org.partiql.value.NullableBoolValue
import org.partiql.value.NullableCharValue
import org.partiql.value.NullableClobValue
import org.partiql.value.NullableDateValue
import org.partiql.value.NullableInt16Value
import org.partiql.value.NullableInt32Value
import org.partiql.value.NullableInt8Value
import org.partiql.value.NullableListValue
import org.partiql.value.NullableSexpValue
import org.partiql.value.NullableStringValue
import org.partiql.value.NullableSymbolValue
import org.partiql.value.NullableTimeValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
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
import java.util.ServiceLoader

class ServiceLoaderUtil {
    companion object {
        @JvmStatic
        fun loadPlugins(): List<ExprFunction> {
            val serviceLoader = ServiceLoader.load(Plugin::class.java)
            return serviceLoader
                .flatMap { serviceLoader -> serviceLoader.getFunctions() }
                .flatMap { partiqlFunc -> PartiQLtoExprFunction(partiqlFunc) }
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun PartiQLtoExprFunction(customFunction: PartiQLFunction): List<ExprFunction> {
            val names = customFunction.signature.names
            val parameters = customFunction.signature.parameters.filterIsInstance<Parameter.ValueParameter>().map { it.type }
            val returnType = customFunction.signature.returns
            val exprFuncs = mutableListOf<ExprFunction>()
            for (name in names) {
                exprFuncs.add(
                    object : ExprFunction {
                        override val signature = FunctionSignature(
                            name = name,
                            requiredParameters = parameters.map { PartiQLToStaticType(it) },
                            returnType = PartiQLToStaticType(returnType),
                        )

                        override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
                            val partiQLArguments = required.mapIndexed { i, expr -> ExprToPartiQLValue(expr, parameters[i]) }
                            val partiQLResult = customFunction.invoke(session.toConnectorSession(), partiQLArguments) // connections from EvaluationSession to ConnectorSession
                            return PartiQLtoExprValue(partiQLResult)
                        }
                    }
                )
            }
            return exprFuncs
        }

        private fun PartiQLToStaticType(partiqlType: PartiQLValueType): StaticType {
            return when (partiqlType) {
                PartiQLValueType.BOOL -> StaticType.BOOL
                PartiQLValueType.INT8 -> StaticType.INT
                PartiQLValueType.INT16 -> StaticType.INT2
                PartiQLValueType.INT32 -> StaticType.INT4
                PartiQLValueType.INT64 -> StaticType.INT8
                PartiQLValueType.INT -> StaticType.INT
                PartiQLValueType.DECIMAL -> StaticType.DECIMAL
                PartiQLValueType.FLOAT32 -> StaticType.FLOAT
                PartiQLValueType.FLOAT64 -> StaticType.FLOAT
                PartiQLValueType.CHAR -> StaticType.STRING
                PartiQLValueType.STRING -> StaticType.STRING
                PartiQLValueType.SYMBOL -> StaticType.SYMBOL
                PartiQLValueType.BINARY -> TODO()
                PartiQLValueType.BYTE -> TODO()
                PartiQLValueType.BLOB -> StaticType.BLOB
                PartiQLValueType.CLOB -> StaticType.CLOB
                PartiQLValueType.DATE -> StaticType.DATE
                PartiQLValueType.TIME -> StaticType.TIME
                PartiQLValueType.TIMESTAMP -> StaticType.TIMESTAMP
                PartiQLValueType.INTERVAL -> TODO()
                PartiQLValueType.BAG -> StaticType.BAG
                PartiQLValueType.LIST -> StaticType.LIST
                PartiQLValueType.SEXP -> StaticType.SEXP
                PartiQLValueType.STRUCT -> StaticType.STRUCT
                PartiQLValueType.NULL -> StaticType.NULL
                PartiQLValueType.MISSING -> StaticType.MISSING
                PartiQLValueType.NULLABLE_BOOL -> StaticType.BOOL.asNullable()
                PartiQLValueType.NULLABLE_INT8 -> StaticType.INT.asNullable()
                PartiQLValueType.NULLABLE_INT16 -> StaticType.INT2.asNullable()
                PartiQLValueType.NULLABLE_INT32 -> StaticType.INT4.asNullable()
                PartiQLValueType.NULLABLE_INT64 -> StaticType.INT8.asNullable()
                PartiQLValueType.NULLABLE_INT -> StaticType.INT.asNullable()
                PartiQLValueType.NULLABLE_DECIMAL -> StaticType.DECIMAL.asNullable()
                PartiQLValueType.NULLABLE_FLOAT32 -> StaticType.FLOAT.asNullable()
                PartiQLValueType.NULLABLE_FLOAT64 -> StaticType.FLOAT.asNullable()
                PartiQLValueType.NULLABLE_CHAR -> StaticType.STRING.asNullable()
                PartiQLValueType.NULLABLE_STRING -> StaticType.STRING.asNullable()
                PartiQLValueType.NULLABLE_SYMBOL -> StaticType.SYMBOL.asNullable()
                PartiQLValueType.NULLABLE_BINARY -> TODO()
                PartiQLValueType.NULLABLE_BYTE -> TODO()
                PartiQLValueType.NULLABLE_BLOB -> StaticType.BLOB.asNullable()
                PartiQLValueType.NULLABLE_CLOB -> StaticType.CLOB.asNullable()
                PartiQLValueType.NULLABLE_DATE -> StaticType.DATE.asNullable()
                PartiQLValueType.NULLABLE_TIME -> StaticType.TIME.asNullable()
                PartiQLValueType.NULLABLE_TIMESTAMP -> StaticType.TIMESTAMP.asNullable()
                PartiQLValueType.NULLABLE_INTERVAL -> TODO()
                PartiQLValueType.NULLABLE_BAG -> StaticType.BAG.asNullable()
                PartiQLValueType.NULLABLE_LIST -> StaticType.LIST.asNullable()
                PartiQLValueType.NULLABLE_SEXP -> StaticType.SEXP.asNullable()
                PartiQLValueType.NULLABLE_STRUCT -> StaticType.STRUCT.asNullable()
            }
        }

        @Throws(PartiQLtoExprValueNullException::class)
        @OptIn(PartiQLValueExperimental::class)
        private fun PartiQLtoExprValue(partiqlValue: PartiQLValue): ExprValue {
            return when (partiqlValue.type) {
                PartiQLValueType.BOOL -> (partiqlValue as? BoolValue)?.value?.let { newBoolean(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.INT8 -> (partiqlValue as? Int8Value)?.int?.let { newInt(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.INT16 -> (partiqlValue as? Int16Value)?.int?.let { newInt(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.INT32 -> (partiqlValue as? Int32Value)?.int?.let { newInt(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.INT64 -> (partiqlValue as? Int64Value)?.long?.let { newInt(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.INT -> (partiqlValue as? IntValue)?.long?.let { newInt(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.DECIMAL -> (partiqlValue as? DecimalValue)?.value?.let { newDecimal(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.FLOAT32 -> (partiqlValue as? Float32Value)?.double?.let { newFloat(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.FLOAT64 -> (partiqlValue as? Float64Value)?.double?.let { newFloat(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.CHAR -> (partiqlValue as? CharValue)?.string?.let { newString(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.STRING -> (partiqlValue as? StringValue)?.string?.let { newString(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.SYMBOL -> (partiqlValue as? SymbolValue)?.string?.let { newSymbol(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.BINARY -> TODO()

                PartiQLValueType.BYTE -> TODO()

                PartiQLValueType.BLOB -> (partiqlValue as? BlobValue)?.value?.let { newBlob(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.CLOB -> (partiqlValue as? ClobValue)?.value?.let { newClob(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.DATE -> (partiqlValue as? DateValue)?.value?.let { newDate(it) }
                    ?: throw PartiQLtoExprValueNullException()

                PartiQLValueType.TIME -> {
                    val timeValue = partiqlValue as? TimeValue
                    timeValue?.let { tv ->
                        val value = tv.value
                        val precision = tv.precision
                        val offset = tv.offset
                        val withzone = tv.withZone
                        if (withzone) {
                            offset?.let {
                                newTime(Time.of(value, precision, it))
                            }
                        } else {
                            newTime(Time.of(value, precision, null))
                        }
                    } ?: throw PartiQLtoExprValueNullException()
                }

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
                    (partiqlValue as? BagValue<*>)?.elements?.map { PartiQLtoExprValue(it) }
                        ?.let { newBag(it.asSequence()) }
                        ?: throw PartiQLtoExprValueNullException()
                }

                PartiQLValueType.LIST -> {
                    (partiqlValue as? ListValue<*>)?.elements?.map { PartiQLtoExprValue(it) }
                        ?.let { newList(it.asSequence()) }
                        ?: throw PartiQLtoExprValueNullException()
                }

                PartiQLValueType.SEXP -> {
                    (partiqlValue as? SexpValue<*>)?.elements?.map { PartiQLtoExprValue(it) }
                        ?.let { newSexp(it.asSequence()) }
                        ?: throw PartiQLtoExprValueNullException()
                }

                PartiQLValueType.STRUCT -> {
                    (partiqlValue as? StructValue<*>)?.fields?.map { PartiQLtoExprValue(it.second).namedValue(newString(it.first)) }
                        ?.let { newStruct(it.asSequence(), StructOrdering.ORDERED) }
                        ?: throw PartiQLtoExprValueNullException()
                }

                PartiQLValueType.NULL -> ExprValue.nullValue

                PartiQLValueType.MISSING -> ExprValue.missingValue

                PartiQLValueType.NULLABLE_BOOL -> {
                    (partiqlValue as? NullableBoolValue)?.value?.let { newBoolean(it) }
                        ?: ExprValue.nullValue
                }

                PartiQLValueType.NULLABLE_INT8 -> (partiqlValue as? NullableInt8Value)?.int?.let { newInt(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_INT16 -> (partiqlValue as? NullableInt16Value)?.int?.let { newInt(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_INT32 -> (partiqlValue as? NullableInt32Value)?.int?.let { newInt(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_INT64 -> (partiqlValue as? Int64Value)?.long?.let { newInt(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_INT -> (partiqlValue as? IntValue)?.long?.let { newInt(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_DECIMAL -> (partiqlValue as? DecimalValue)?.value?.let { newDecimal(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_FLOAT32 -> (partiqlValue as? Float32Value)?.double?.let { newFloat(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_FLOAT64 -> (partiqlValue as? Float32Value)?.double?.let { newFloat(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_CHAR -> (partiqlValue as? NullableCharValue)?.string?.let { newString(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_STRING -> (partiqlValue as? NullableStringValue)?.string?.let { newString(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_SYMBOL -> (partiqlValue as? NullableSymbolValue)?.string?.let { newSymbol(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_BINARY -> TODO()

                PartiQLValueType.NULLABLE_BYTE -> TODO()

                PartiQLValueType.NULLABLE_BLOB -> (partiqlValue as? NullableBlobValue)?.value?.let { newBlob(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_CLOB -> (partiqlValue as? NullableClobValue)?.value?.let { newClob(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_DATE -> (partiqlValue as? NullableDateValue)?.value?.let { newDate(it) }
                    ?: ExprValue.nullValue

                PartiQLValueType.NULLABLE_TIME -> {
                    (partiqlValue as? NullableTimeValue)?.let { tv ->
                        tv.value?.let { value ->
                            val precision = tv.precision
                            val offset = tv.offset
                            val withzone = tv.withZone
                            if (withzone) {
                                offset?.let { offsetValue -> newTime(Time.of(value, precision, offsetValue)) } ?: null
                            } else {
                                newTime(Time.of(value, precision, null))
                            }
                        }
                    } ?: ExprValue.nullValue
                }

                PartiQLValueType.NULLABLE_TIMESTAMP -> TODO()
                // TODO: Implement
//          {
//                (partiqlValue as? NullableTimestampValue)?.let { tv ->
//                    tv.value?.let { localDateTime ->
//                        val zoneOffset = tv.offset
//                        val instant = localDateTime.atOffset(zoneOffset ?: ZoneOffset.UTC).toInstant()
//                        val timestamp = Timestamp.forMillis(instant.toEpochMilli(), (zoneOffset?.totalSeconds ?: 0) / 60)
//                        newTimestamp(timestamp)
//                    }
//                } ?: ExprValue.nullValue
//            }

                PartiQLValueType.NULLABLE_INTERVAL -> TODO() // add nullable interval conversion

                PartiQLValueType.NULLABLE_BAG -> {
                    (partiqlValue as? NullableBagValue<*>)?.elements?.map { PartiQLtoExprValue(it) }
                        ?.let { newBag(it.asSequence()) } ?: ExprValue.nullValue
                }

                PartiQLValueType.NULLABLE_LIST -> {
                    (partiqlValue as? NullableListValue<*>)?.elements?.map { PartiQLtoExprValue(it) }
                        ?.let { newList(it.asSequence()) } ?: ExprValue.nullValue
                }

                PartiQLValueType.NULLABLE_SEXP -> {
                    (partiqlValue as? NullableSexpValue<*>)?.elements?.map { PartiQLtoExprValue(it) }
                        ?.let { newSexp(it.asSequence()) } ?: ExprValue.nullValue
                }

                PartiQLValueType.NULLABLE_STRUCT -> {
                    (partiqlValue as? StructValue<*>)?.fields?.map { PartiQLtoExprValue(it.second).namedValue(newString(it.first)) }
                        ?.let { newStruct(it.asSequence(), StructOrdering.ORDERED) } ?: ExprValue.nullValue
                }
            }
        }

        @Throws(ExprToPartiQLValueIntException::class, ExprToPartiQLValueFloatException::class)
        @OptIn(PartiQLValueExperimental::class)
        private fun ExprToPartiQLValue(exprValue: ExprValue, partiqlType: PartiQLValueType): PartiQLValue {
            return when (exprValue.type) {
                ExprValueType.MISSING -> missingValue()
                ExprValueType.NULL -> nullValue()
                ExprValueType.BOOL -> boolValue(exprValue.booleanValue())
                ExprValueType.INT -> when (partiqlType) {
                    PartiQLValueType.INT8 -> int8Value(exprValue.numberValue().toByte())
                    PartiQLValueType.INT16 -> int16Value(exprValue.numberValue().toShort())
                    PartiQLValueType.INT32 -> int32Value(exprValue.numberValue().toInt())
                    PartiQLValueType.INT64 -> int64Value(exprValue.numberValue().toLong())
                    PartiQLValueType.INT -> intValue(exprValue.numberValue() as BigInteger)
                    else -> throw ExprToPartiQLValueIntException()
                }

                ExprValueType.FLOAT -> when (partiqlType) {
                    PartiQLValueType.FLOAT32 -> float32Value(exprValue.numberValue().toFloat())
                    PartiQLValueType.FLOAT64 -> float64Value(exprValue.numberValue().toDouble())
                    else -> throw ExprToPartiQLValueFloatException()
                }

                ExprValueType.DECIMAL -> decimalValue(exprValue.numberValue() as BigDecimal)
                ExprValueType.DATE -> dateValue(exprValue.dateValue())
                ExprValueType.TIMESTAMP -> TODO()
                ExprValueType.TIME -> timeValue(exprValue.timeValue().localTime, exprValue.timeValue().precision, exprValue.timeValue().zoneOffset, true)
                ExprValueType.SYMBOL -> symbolValue(exprValue.stringValue())
                ExprValueType.STRING -> stringValue(exprValue.stringValue())
                ExprValueType.STRING -> when (partiqlType) {
                    PartiQLValueType.STRING -> stringValue(exprValue.stringValue())
                    else -> charValue(exprValue.stringValue().first())
                }

                ExprValueType.CLOB -> clobValue(exprValue.bytesValue())
                ExprValueType.BLOB -> blobValue(exprValue.bytesValue())
                ExprValueType.LIST -> listValue(exprValue.map { ExprToPartiQLValue(it, ExprToPartiQLValueType(it)) })
                ExprValueType.SEXP -> sexpValue(exprValue.map { ExprToPartiQLValue(it, ExprToPartiQLValueType(it)) })
                ExprValueType.STRUCT -> structValue(exprValue.map { Pair(it.name?.stringValue() ?: "", ExprToPartiQLValue(it, ExprToPartiQLValueType(it))) })
                ExprValueType.BAG -> bagValue(exprValue.map { ExprToPartiQLValue(it, ExprToPartiQLValueType(it)) })
                ExprValueType.GRAPH -> TODO()
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
