package org.partiql.eval.internal.operator.rex

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.plan.Ref
import org.partiql.types.PType
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
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.TextValue
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
    override fun eval(env: Environment): Datum {
        val arg = arg.eval(env).toPartiQLValue()
        try {
            val partiqlValue = when (PType.fromPartiQLValueType(arg.type).kind) {
                PType.Kind.DYNAMIC -> TODO("Not Possible")
                PType.Kind.BOOL -> castFromBool(arg as BoolValue, cast.target)
                PType.Kind.TINYINT -> castFromNumeric(arg as Int8Value, cast.target)
                PType.Kind.SMALLINT -> castFromNumeric(arg as Int16Value, cast.target)
                PType.Kind.INT -> castFromNumeric(arg as Int32Value, cast.target)
                PType.Kind.BIGINT -> castFromNumeric(arg as Int64Value, cast.target)
                PType.Kind.INT_ARBITRARY -> castFromNumeric(arg as IntValue, cast.target)
                PType.Kind.DECIMAL -> castFromNumeric(arg as DecimalValue, cast.target)
                PType.Kind.DECIMAL_ARBITRARY -> castFromNumeric(arg as DecimalValue, cast.target)
                PType.Kind.REAL -> castFromNumeric(arg as Float32Value, cast.target)
                PType.Kind.DOUBLE_PRECISION -> castFromNumeric(arg as Float64Value, cast.target)
                PType.Kind.CHAR -> TODO("Char value implementation is wrong")
                PType.Kind.STRING -> castFromText(arg as StringValue, cast.target)
                PType.Kind.SYMBOL -> castFromText(arg as SymbolValue, cast.target)
                PType.Kind.BLOB -> TODO("CAST FROM BLOB not yet implemented")
                PType.Kind.CLOB -> TODO("CAST FROM CLOB not yet implemented")
                PType.Kind.DATE -> TODO("CAST FROM DATE not yet implemented")
                PType.Kind.TIME_WITH_TZ -> TODO("CAST FROM TIME not yet implemented")
                PType.Kind.TIME_WITHOUT_TZ -> TODO("CAST FROM TIME not yet implemented")
                PType.Kind.TIMESTAMP_WITH_TZ -> TODO("CAST FROM TIMESTAMP not yet implemented")
                PType.Kind.TIMESTAMP_WITHOUT_TZ -> TODO("CAST FROM TIMESTAMP not yet implemented")
                PType.Kind.BAG -> castFromCollection(arg as BagValue<*>, cast.target)
                PType.Kind.LIST -> castFromCollection(arg as ListValue<*>, cast.target)
                PType.Kind.SEXP -> castFromCollection(arg as SexpValue<*>, cast.target)
                PType.Kind.STRUCT -> TODO("CAST FROM STRUCT not yet implemented")
                PType.Kind.ROW -> TODO("CAST FROM ROW not yet implemented")
                PType.Kind.UNKNOWN -> TODO()
            }
            return Datum.of(partiqlValue)
        } catch (e: DataException) {
            throw TypeCheckException()
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun castFromNull(value: NullValue, t: PType): PartiQLValue {
        return when (t.kind) {
            PType.Kind.DYNAMIC -> value
            PType.Kind.BOOL -> boolValue(null)
            PType.Kind.CHAR -> charValue(null)
            PType.Kind.STRING -> stringValue(null)
            PType.Kind.SYMBOL -> symbolValue(null)
            PType.Kind.BLOB -> blobValue(null)
            PType.Kind.CLOB -> clobValue(null)
            PType.Kind.DATE -> dateValue(null)
            PType.Kind.TIME_WITH_TZ -> timeValue(null) // TODO
            PType.Kind.TIME_WITHOUT_TZ -> timeValue(null)
            PType.Kind.TIMESTAMP_WITH_TZ -> timestampValue(null) // TODO
            PType.Kind.TIMESTAMP_WITHOUT_TZ -> timestampValue(null)
            PType.Kind.BAG -> bagValue<PartiQLValue>(null)
            PType.Kind.LIST -> listValue<PartiQLValue>(null)
            PType.Kind.SEXP -> sexpValue<PartiQLValue>(null)
            PType.Kind.STRUCT -> structValue<PartiQLValue>(null)
            PType.Kind.TINYINT -> int8Value(null)
            PType.Kind.SMALLINT -> int16Value(null)
            PType.Kind.INT -> int32Value(null)
            PType.Kind.BIGINT -> int64Value(null)
            PType.Kind.INT_ARBITRARY -> intValue(null)
            PType.Kind.DECIMAL -> decimalValue(null)
            PType.Kind.DECIMAL_ARBITRARY -> decimalValue(null)
            PType.Kind.REAL -> float32Value(null)
            PType.Kind.DOUBLE_PRECISION -> float64Value(null)
            PType.Kind.ROW -> structValue<PartiQLValue>(null) // TODO. PartiQLValue doesn't have rows.
            PType.Kind.UNKNOWN -> TODO()
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun castFromBool(value: BoolValue, t: PType): PartiQLValue {
        val v = value.value
        return when (t.kind) {
            PType.Kind.DYNAMIC -> value
            PType.Kind.BOOL -> value
            PType.Kind.TINYINT -> when (v) {
                true -> int8Value(1)
                false -> int8Value(0)
                null -> int8Value(null)
            }

            PType.Kind.SMALLINT -> when (v) {
                true -> int16Value(1)
                false -> int16Value(0)
                null -> int16Value(null)
            }

            PType.Kind.INT -> when (v) {
                true -> int32Value(1)
                false -> int32Value(0)
                null -> int32Value(null)
            }

            PType.Kind.BIGINT -> when (v) {
                true -> int64Value(1)
                false -> int64Value(0)
                null -> int64Value(null)
            }

            PType.Kind.INT_ARBITRARY -> when (v) {
                true -> intValue(BigInteger.valueOf(1))
                false -> intValue(BigInteger.valueOf(0))
                null -> intValue(null)
            }

            PType.Kind.DECIMAL, PType.Kind.DECIMAL_ARBITRARY -> when (v) {
                true -> decimalValue(BigDecimal.ONE)
                false -> decimalValue(BigDecimal.ZERO)
                null -> decimalValue(null)
            }

            PType.Kind.REAL -> {
                when (v) {
                    true -> float32Value(1.0.toFloat())
                    false -> float32Value(0.0.toFloat())
                    null -> float32Value(null)
                }
            }

            PType.Kind.DOUBLE_PRECISION -> when (v) {
                true -> float64Value(1.0)
                false -> float64Value(0.0)
                null -> float64Value(null)
            }

            PType.Kind.CHAR -> TODO("Char value implementation is wrong")
            PType.Kind.STRING -> stringValue(v?.toString())
            PType.Kind.SYMBOL -> symbolValue(v?.toString())
            PType.Kind.BLOB, PType.Kind.CLOB,
            PType.Kind.DATE, PType.Kind.TIMESTAMP_WITH_TZ, PType.Kind.TIMESTAMP_WITHOUT_TZ, PType.Kind.TIME_WITH_TZ,
            PType.Kind.TIME_WITHOUT_TZ, PType.Kind.BAG, PType.Kind.LIST,
            PType.Kind.SEXP,
            PType.Kind.ROW,
            PType.Kind.STRUCT -> error("can not perform cast from $value to $t")
            PType.Kind.UNKNOWN -> TODO()
        }
    }
    @OptIn(PartiQLValueExperimental::class)
    private fun castFromNumeric(value: NumericValue<*>, t: PType): PartiQLValue {
        val v = value.value
        return when (t.kind) {
            PType.Kind.DYNAMIC -> value
            PType.Kind.BOOL -> when {
                v == null -> boolValue(null)
                v == 0.0 -> boolValue(false)
                else -> boolValue(true)
            }
            PType.Kind.TINYINT -> value.toInt8()
            PType.Kind.SMALLINT -> value.toInt16()
            PType.Kind.INT -> value.toInt32()
            PType.Kind.BIGINT -> value.toInt64()
            PType.Kind.INT_ARBITRARY -> value.toInt()
            PType.Kind.DECIMAL -> value.toDecimal()
            PType.Kind.DECIMAL_ARBITRARY -> value.toDecimal()
            PType.Kind.REAL -> value.toFloat32()
            PType.Kind.DOUBLE_PRECISION -> value.toFloat64()
            PType.Kind.CHAR -> TODO("Char value implementation is wrong")
            PType.Kind.STRING -> stringValue(v?.toString(), value.annotations)
            PType.Kind.SYMBOL -> symbolValue(v?.toString(), value.annotations)
            PType.Kind.BLOB, PType.Kind.CLOB,
            PType.Kind.DATE, PType.Kind.TIME_WITH_TZ, PType.Kind.TIME_WITHOUT_TZ, PType.Kind.TIMESTAMP_WITH_TZ,
            PType.Kind.TIMESTAMP_WITHOUT_TZ,
            PType.Kind.BAG, PType.Kind.LIST,
            PType.Kind.SEXP,
            PType.Kind.STRUCT -> error("can not perform cast from $value to $t")
            PType.Kind.ROW -> error("can not perform cast from $value to $t")
            PType.Kind.UNKNOWN -> TODO()
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun castFromText(value: TextValue<String>, t: PType): PartiQLValue {
        return when (t.kind) {
            PType.Kind.DYNAMIC -> value
            PType.Kind.BOOL -> {
                val str = value.value?.lowercase() ?: return boolValue(null, value.annotations)
                if (str == "true") return boolValue(true, value.annotations)
                if (str == "false") return boolValue(false, value.annotations)
                throw TypeCheckException()
            }
            PType.Kind.TINYINT -> {
                val stringValue = value.value ?: return int8Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt8()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.SMALLINT -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt16()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.INT -> {
                val stringValue = value.value ?: return int32Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt32()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.BIGINT -> {
                val stringValue = value.value ?: return int64Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt64()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.INT_ARBITRARY -> {
                val stringValue = value.value ?: return intValue(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.DECIMAL -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Decimal -> decimalValue(number, value.annotations).toDecimal()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.DECIMAL_ARBITRARY -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Decimal -> decimalValue(number, value.annotations).toDecimal()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.REAL -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Double -> float64Value(number, value.annotations).toFloat32()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.DOUBLE_PRECISION -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Double -> float64Value(number, value.annotations).toFloat32()
                    else -> throw TypeCheckException()
                }
            }
            PType.Kind.CHAR -> TODO("Char value implementation is wrong")
            PType.Kind.STRING -> stringValue(value.value, value.annotations)
            PType.Kind.SYMBOL -> symbolValue(value.value, value.annotations)
            PType.Kind.BLOB, PType.Kind.CLOB,
            PType.Kind.DATE, PType.Kind.TIME_WITH_TZ, PType.Kind.TIME_WITHOUT_TZ, PType.Kind.TIMESTAMP_WITH_TZ,
            PType.Kind.TIMESTAMP_WITHOUT_TZ,
            PType.Kind.BAG, PType.Kind.LIST,
            PType.Kind.SEXP,
            PType.Kind.STRUCT -> error("can not perform cast from struct to $t")
            PType.Kind.ROW -> error("can not perform cast from $value to $t")
            PType.Kind.UNKNOWN -> TODO()
        }
    }

    // TODO: Fix NULL Collection
    @OptIn(PartiQLValueExperimental::class)
    private fun castFromCollection(value: CollectionValue<*>, t: PType): PartiQLValue {
        val elements = mutableListOf<PartiQLValue>()
        value.iterator().forEachRemaining {
            elements.add(it)
        }
        return when (t.kind) {
            PType.Kind.BAG -> bagValue(elements)
            PType.Kind.LIST -> listValue(elements)
            PType.Kind.SEXP -> sexpValue(elements)
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
