package org.partiql.eval.internal.operator.rex

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Ref
import org.partiql.value.BagValue
import org.partiql.value.BoolValue
import org.partiql.value.CollectionValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.ListValue
import org.partiql.value.NullValue
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.TextValue
import org.partiql.value.bagValue
import org.partiql.value.binaryValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.byteValue
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
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue
import java.math.BigDecimal
import java.math.BigInteger

// TODO: This is incomplete
internal class ExprCast(val arg: Operator.Expr, val cast: Ref.Cast) : Operator.Expr {
    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PQLValue {
        val arg = arg.eval(env)
        try {
            val partiqlValue = when (arg.type) {
                PartiQLValueType.ANY -> TODO("Not Possible")
                PartiQLValueType.BOOL -> castFromBool(arg as BoolValue, cast.target)
                PartiQLValueType.INT8 -> castFromNumeric(arg as Int8Value, cast.target)
                PartiQLValueType.INT16 -> castFromNumeric(arg as Int16Value, cast.target)
                PartiQLValueType.INT32 -> castFromNumeric(arg as Int32Value, cast.target)
                PartiQLValueType.INT64 -> castFromNumeric(arg as Int64Value, cast.target)
                PartiQLValueType.INT -> castFromNumeric(arg as IntValue, cast.target)
                PartiQLValueType.DECIMAL -> castFromNumeric(arg as DecimalValue, cast.target)
                PartiQLValueType.DECIMAL_ARBITRARY -> castFromNumeric(arg as DecimalValue, cast.target)
                PartiQLValueType.FLOAT32 -> castFromNumeric(arg as Float32Value, cast.target)
                PartiQLValueType.FLOAT64 -> castFromNumeric(arg as Float64Value, cast.target)
                PartiQLValueType.CHAR -> TODO("Char value implementation is wrong")
                PartiQLValueType.STRING -> castFromText(arg as StringValue, cast.target)
                PartiQLValueType.SYMBOL -> castFromText(arg as SymbolValue, cast.target)
                PartiQLValueType.BINARY -> TODO("Static Type does not support Binary")
                PartiQLValueType.BYTE -> TODO("Static Type does not support Byte")
                PartiQLValueType.BLOB -> TODO("CAST FROM BLOB not yet implemented")
                PartiQLValueType.CLOB -> TODO("CAST FROM CLOB not yet implemented")
                PartiQLValueType.DATE -> TODO("CAST FROM DATE not yet implemented")
                PartiQLValueType.TIME -> TODO("CAST FROM TIME not yet implemented")
                PartiQLValueType.TIMESTAMP -> TODO("CAST FROM TIMESTAMP not yet implemented")
                PartiQLValueType.INTERVAL -> TODO("Static Type does not support INTERVAL")
                PartiQLValueType.BAG -> castFromCollection(arg as BagValue<*>, cast.target)
                PartiQLValueType.LIST -> castFromCollection(arg as ListValue<*>, cast.target)
                PartiQLValueType.SEXP -> castFromCollection(arg as SexpValue<*>, cast.target)
                PartiQLValueType.STRUCT -> TODO("CAST FROM STRUCT not yet implemented")
                PartiQLValueType.NULL -> castFromNull(arg as NullValue, cast.target)
                PartiQLValueType.MISSING -> error("cast from MISSING should be handled by Typer")
            }
            return PQLValue.of(partiqlValue)
        } catch (e: DataException) {
            throw TypeCheckException()
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun castFromNull(value: NullValue, t: PartiQLValueType): PartiQLValue {
        return when (t) {
            PartiQLValueType.ANY -> value
            PartiQLValueType.BOOL -> boolValue(null)
            PartiQLValueType.CHAR -> charValue(null)
            PartiQLValueType.STRING -> stringValue(null)
            PartiQLValueType.SYMBOL -> symbolValue(null)
            PartiQLValueType.BINARY -> binaryValue(null)
            PartiQLValueType.BYTE -> byteValue(null)
            PartiQLValueType.BLOB -> blobValue(null)
            PartiQLValueType.CLOB -> clobValue(null)
            PartiQLValueType.DATE -> dateValue(null)
            PartiQLValueType.TIME -> timeValue(null)
            PartiQLValueType.TIMESTAMP -> timestampValue(null)
            PartiQLValueType.INTERVAL -> TODO("Not yet supported")
            PartiQLValueType.BAG -> bagValue<PartiQLValue>(null)
            PartiQLValueType.LIST -> listValue<PartiQLValue>(null)
            PartiQLValueType.SEXP -> sexpValue<PartiQLValue>(null)
            PartiQLValueType.STRUCT -> structValue<PartiQLValue>(null)
            PartiQLValueType.NULL -> value
            PartiQLValueType.MISSING -> missingValue() // TODO: Os this allowed
            PartiQLValueType.INT8 -> int8Value(null)
            PartiQLValueType.INT16 -> int16Value(null)
            PartiQLValueType.INT32 -> int32Value(null)
            PartiQLValueType.INT64 -> int64Value(null)
            PartiQLValueType.INT -> intValue(null)
            PartiQLValueType.DECIMAL -> decimalValue(null)
            PartiQLValueType.DECIMAL_ARBITRARY -> decimalValue(null)
            PartiQLValueType.FLOAT32 -> float32Value(null)
            PartiQLValueType.FLOAT64 -> float64Value(null)
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun castFromBool(value: BoolValue, t: PartiQLValueType): PartiQLValue {
        val v = value.value
        return when (t) {
            PartiQLValueType.ANY -> value
            PartiQLValueType.BOOL -> value
            PartiQLValueType.INT8 -> when (v) {
                true -> int8Value(1)
                false -> int8Value(0)
                null -> int8Value(null)
            }

            PartiQLValueType.INT16 -> when (v) {
                true -> int16Value(1)
                false -> int16Value(0)
                null -> int16Value(null)
            }

            PartiQLValueType.INT32 -> when (v) {
                true -> int32Value(1)
                false -> int32Value(0)
                null -> int32Value(null)
            }

            PartiQLValueType.INT64 -> when (v) {
                true -> int64Value(1)
                false -> int64Value(0)
                null -> int64Value(null)
            }

            PartiQLValueType.INT -> when (v) {
                true -> intValue(BigInteger.valueOf(1))
                false -> intValue(BigInteger.valueOf(0))
                null -> intValue(null)
            }

            PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY -> when (v) {
                true -> decimalValue(BigDecimal.ONE)
                false -> decimalValue(BigDecimal.ZERO)
                null -> decimalValue(null)
            }

            PartiQLValueType.FLOAT32 -> {
                when (v) {
                    true -> float32Value(1.0.toFloat())
                    false -> float32Value(0.0.toFloat())
                    null -> float32Value(null)
                }
            }

            PartiQLValueType.FLOAT64 -> when (v) {
                true -> float64Value(1.0)
                false -> float64Value(0.0)
                null -> float64Value(null)
            }

            PartiQLValueType.CHAR -> TODO("Char value implementation is wrong")
            PartiQLValueType.STRING -> stringValue(v?.toString())
            PartiQLValueType.SYMBOL -> symbolValue(v?.toString())
            PartiQLValueType.BINARY, PartiQLValueType.BYTE,
            PartiQLValueType.BLOB, PartiQLValueType.CLOB,
            PartiQLValueType.DATE, PartiQLValueType.TIME, PartiQLValueType.TIMESTAMP,
            PartiQLValueType.INTERVAL,
            PartiQLValueType.BAG, PartiQLValueType.LIST,
            PartiQLValueType.SEXP,
            PartiQLValueType.STRUCT -> error("can not perform cast from $value to $t")
            PartiQLValueType.NULL -> error("cast to null not supported")
            PartiQLValueType.MISSING -> error("cast to missing not supported")
        }
    }
    @OptIn(PartiQLValueExperimental::class)
    private fun castFromNumeric(value: NumericValue<*>, t: PartiQLValueType): PartiQLValue {
        val v = value.value
        return when (t) {
            PartiQLValueType.ANY -> value
            PartiQLValueType.BOOL -> when {
                v == null -> boolValue(null)
                v == 0.0 -> boolValue(false)
                else -> boolValue(true)
            }
            PartiQLValueType.INT8 -> value.toInt8()
            PartiQLValueType.INT16 -> value.toInt16()
            PartiQLValueType.INT32 -> value.toInt32()
            PartiQLValueType.INT64 -> value.toInt64()
            PartiQLValueType.INT -> value.toInt()
            PartiQLValueType.DECIMAL -> value.toDecimal()
            PartiQLValueType.DECIMAL_ARBITRARY -> value.toDecimal()
            PartiQLValueType.FLOAT32 -> value.toFloat32()
            PartiQLValueType.FLOAT64 -> value.toFloat64()
            PartiQLValueType.CHAR -> TODO("Char value implementation is wrong")
            PartiQLValueType.STRING -> stringValue(v?.toString(), value.annotations)
            PartiQLValueType.SYMBOL -> symbolValue(v?.toString(), value.annotations)
            PartiQLValueType.BINARY, PartiQLValueType.BYTE,
            PartiQLValueType.BLOB, PartiQLValueType.CLOB,
            PartiQLValueType.DATE, PartiQLValueType.TIME, PartiQLValueType.TIMESTAMP,
            PartiQLValueType.INTERVAL,
            PartiQLValueType.BAG, PartiQLValueType.LIST,
            PartiQLValueType.SEXP,
            PartiQLValueType.STRUCT -> error("can not perform cast from $value to $t")
            PartiQLValueType.NULL -> error("cast to null not supported")
            PartiQLValueType.MISSING -> error("cast to missing not supported")
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun castFromText(value: TextValue<String>, t: PartiQLValueType): PartiQLValue {
        return when (t) {
            PartiQLValueType.ANY -> value
            PartiQLValueType.BOOL -> {
                val str = value.value?.lowercase() ?: return boolValue(null, value.annotations)
                if (str == "true") return boolValue(true, value.annotations)
                if (str == "false") return boolValue(false, value.annotations)
                throw TypeCheckException()
            }
            PartiQLValueType.INT8 -> {
                val stringValue = value.value ?: return int8Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt8()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.INT16 -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt16()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.INT32 -> {
                val stringValue = value.value ?: return int32Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt32()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.INT64 -> {
                val stringValue = value.value ?: return int64Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt64()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.INT -> {
                val stringValue = value.value ?: return intValue(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.DECIMAL -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Decimal -> decimalValue(number, value.annotations).toDecimal()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.DECIMAL_ARBITRARY -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Decimal -> decimalValue(number, value.annotations).toDecimal()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.FLOAT32 -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Double -> float64Value(number, value.annotations).toFloat32()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.FLOAT64 -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Double -> float64Value(number, value.annotations).toFloat32()
                    else -> throw TypeCheckException()
                }
            }
            PartiQLValueType.CHAR -> TODO("Char value implementation is wrong")
            PartiQLValueType.STRING -> stringValue(value.value, value.annotations)
            PartiQLValueType.SYMBOL -> symbolValue(value.value, value.annotations)
            PartiQLValueType.BINARY, PartiQLValueType.BYTE,
            PartiQLValueType.BLOB, PartiQLValueType.CLOB,
            PartiQLValueType.DATE, PartiQLValueType.TIME, PartiQLValueType.TIMESTAMP,
            PartiQLValueType.INTERVAL,
            PartiQLValueType.BAG, PartiQLValueType.LIST,
            PartiQLValueType.SEXP,
            PartiQLValueType.STRUCT -> error("can not perform cast from struct to $t")
            PartiQLValueType.NULL -> error("cast to null not supported")
            PartiQLValueType.MISSING -> error("cast to missing not supported")
        }
    }

    // TODO: Fix NULL Collection
    @OptIn(PartiQLValueExperimental::class)
    private fun castFromCollection(value: CollectionValue<*>, t: PartiQLValueType): PartiQLValue {
        val elements = mutableListOf<PartiQLValue>()
        value.iterator().forEachRemaining {
            elements.add(it)
        }
        return when (t) {
            PartiQLValueType.BAG -> bagValue(elements)
            PartiQLValueType.LIST -> listValue(elements)
            PartiQLValueType.SEXP -> sexpValue(elements)
            else -> error("can not perform cast from $value to $t")
        }
    }

    // For now, utilize ion to parse string such as 0b10, etc.
    private fun getNumberValueFromString(str: String): Number? {
        val ion = try {
            str.let { createIonElementLoader().loadSingleElement(it.normalizeForCastToInt()) }
        } catch (e: IonElementException) {
            throw TypeCheckException()
        }
        return when (ion.type) {
            ElementType.INT -> ion.bigIntegerValueOrNull
            ElementType.FLOAT -> ion.doubleValueOrNull
            ElementType.DECIMAL -> ion.decimalValueOrNull
            else -> null
        }
    }

    private fun String.normalizeForCastToInt(): String {
        fun Char.isSign() = this == '-' || this == '+'
        fun Char.isHexOrBase2Marker(): Boolean {
            val c = this.lowercaseChar()

            return c == 'x' || c == 'b'
        }

        fun String.possiblyHexOrBase2() = (length >= 2 && this[1].isHexOrBase2Marker()) ||
            (length >= 3 && this[0].isSign() && this[2].isHexOrBase2Marker())

        return when {
            length == 0 -> this
            possiblyHexOrBase2() -> {
                if (this[0] == '+') {
                    this.drop(1)
                } else {
                    this
                }
            }
            else -> {
                val (isNegative, startIndex) = when (this[0]) {
                    '-' -> Pair(true, 1)
                    '+' -> Pair(false, 1)
                    else -> Pair(false, 0)
                }

                var toDrop = startIndex
                while (toDrop < length && this[toDrop] == '0') {
                    toDrop += 1
                }

                when {
                    toDrop == length -> "0" // string is all zeros
                    toDrop == 0 -> this
                    toDrop == 1 && isNegative -> this
                    toDrop > 1 && isNegative -> '-' + this.drop(toDrop)
                    else -> this.drop(toDrop)
                }
            }
        }
    }
}
