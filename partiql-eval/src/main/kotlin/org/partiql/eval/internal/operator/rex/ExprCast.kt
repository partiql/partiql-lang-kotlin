package org.partiql.eval.internal.operator.rex

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Ref
import org.partiql.value.ArrayType
import org.partiql.value.BagType
import org.partiql.value.BoolType
import org.partiql.value.BoolValue
import org.partiql.value.CharType
import org.partiql.value.CharVarType
import org.partiql.value.CharVarUnboundedType
import org.partiql.value.CollectionValue
import org.partiql.value.DecimalValue
import org.partiql.value.DynamicType
import org.partiql.value.TypeReal
import org.partiql.value.Float32Value
import org.partiql.value.TypeDoublePrecision
import org.partiql.value.Float64Value
import org.partiql.value.Int16Type
import org.partiql.value.Int16Value
import org.partiql.value.Int32Type
import org.partiql.value.Int32Value
import org.partiql.value.Int64Type
import org.partiql.value.Int64Value
import org.partiql.value.Int8Type
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.MissingType
import org.partiql.value.NullType
import org.partiql.value.NumericType
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.TextValue
import org.partiql.value.TypeIntBig
import org.partiql.value.bagValue
import org.partiql.value.boolValue
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.listValue
import org.partiql.value.stringValue
import java.math.BigDecimal
import java.math.BigInteger

// TODO: This is incomplete
internal class ExprCast(val arg: Operator.Expr, val cast: Ref.Cast) : Operator.Expr {
    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PartiQLValue {
        val arg = arg.eval(env)
        try {
            return when (arg.type) {
                is DynamicType -> TODO("Not Possible")
                is BoolType -> castFromBool(arg as BoolValue, cast.target)
                is Int8Type -> castFromNumeric(arg as Int8Value, cast.target)
                is Int16Type -> castFromNumeric(arg as Int16Value, cast.target)
                is Int32Type -> castFromNumeric(arg as Int32Value, cast.target)
                is Int64Type -> castFromNumeric(arg as Int64Value, cast.target)
                is TypeIntBig -> castFromNumeric(arg as IntValue, cast.target)
                is NumericType -> castFromNumeric(arg as DecimalValue, cast.target)
                is TypeReal -> castFromNumeric(arg as Float32Value, cast.target)
                is TypeDoublePrecision -> castFromNumeric(arg as Float64Value, cast.target)
                is CharType -> TODO("Char value implementation is wrong")
                // TODO: SEXP?
                is ArrayType -> castFromCollection(arg as CollectionValue<*>, cast.target)
                is BagType -> castFromCollection(arg as CollectionValue<*>, cast.target)
                // TODO: Is the following correct? What about symbols?
                is CharVarType, is CharVarUnboundedType -> castFromText(arg as StringValue, cast.target)
                is NullType -> error("cast from NULL should be handled by Typer")
                is MissingType -> error("cast from MISSING should be handled by Typer")
                else -> TODO("CAST from ${arg.type} not yet implemented.")
            }
        } catch (e: DataException) {
            throw TypeCheckException()
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun castFromBool(value: BoolValue, t: PartiQLType): PartiQLValue {
        val v = value.value
        return when (t) {
            is DynamicType -> value
            is BoolType -> value
            is Int8Type -> when (v) {
                true -> int8Value(1)
                false -> int8Value(0)
                null -> int8Value(null)
            }
            is Int16Type -> when (v) {
                true -> int16Value(1)
                false -> int16Value(0)
                null -> int16Value(null)
            }
            is Int32Type -> when (v) {
                true -> int32Value(1)
                false -> int32Value(0)
                null -> int32Value(null)
            }
            is Int64Type -> when (v) {
                true -> int64Value(1)
                false -> int64Value(0)
                null -> int64Value(null)
            }
            is TypeIntBig -> when (v) {
                true -> intValue(BigInteger.ONE)
                false -> intValue(BigInteger.ZERO)
                null -> intValue(null)
            }
            is NumericType -> when (v) {
                true -> decimalValue(BigDecimal.ONE)
                false -> decimalValue(BigDecimal.ZERO)
                null -> decimalValue(null)
            }
            is TypeReal -> {
                when (v) {
                    true -> float32Value(1.0.toFloat())
                    false -> float32Value(0.0.toFloat())
                    null -> float32Value(null)
                }
            }
            is TypeDoublePrecision -> when (v) {
                true -> float64Value(1.0)
                false -> float64Value(0.0)
                null -> float64Value(null)
            }
            is CharType -> TODO("Char value implementation is wrong")
            is CharVarType -> stringValue(v?.toString()) // TODO: Is this right?
            is CharVarUnboundedType -> stringValue(v?.toString()) // TODO: Is this right?
            else -> error("can not perform cast from $value to $t")
        }
    }
    @OptIn(PartiQLValueExperimental::class)
    private fun castFromNumeric(value: NumericValue<*>, t: PartiQLType): PartiQLValue {
        val v = value.value
        return when (t) {
            is DynamicType -> value
            is BoolType -> when {
                v == null -> boolValue(null)
                v == 0.0 -> boolValue(false)
                else -> boolValue(true)
            }
            is Int8Type -> value.toInt8()
            is Int16Type -> value.toInt16()
            is Int32Type -> value.toInt32()
            is Int64Type -> value.toInt64()
            is TypeIntBig -> value.toInt()
            is NumericType -> value.toDecimal()
            is TypeReal -> value.toFloat32()
            is TypeDoublePrecision -> value.toFloat64()
            is CharType -> TODO("Char value implementation is wrong")
            is CharVarType, is CharVarUnboundedType -> stringValue(v?.toString(), value.annotations)
            else -> error("Cannot perform CAST from $value to $t")
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun castFromText(value: TextValue<String>, t: PartiQLType): PartiQLValue {
        return when (t) {
            is DynamicType -> value
            is BoolType -> {
                val str = value.value?.lowercase() ?: return boolValue(null, value.annotations)
                if (str == "true") return boolValue(true, value.annotations)
                if (str == "false") return boolValue(false, value.annotations)
                throw TypeCheckException()
            }
            is Int8Type -> {
                val stringValue = value.value ?: return int8Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt8()
                    else -> throw TypeCheckException()
                }
            }
            is Int16Type -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt16()
                    else -> throw TypeCheckException()
                }
            }
            is Int32Type -> {
                val stringValue = value.value ?: return int32Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt32()
                    else -> throw TypeCheckException()
                }
            }
            is Int64Type -> {
                val stringValue = value.value ?: return int64Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger -> intValue(number, value.annotations).toInt64()
                    else -> throw TypeCheckException()
                }
            }
            is TypeIntBig -> {
                val stringValue = value.value ?: return intValue(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is BigInteger? -> intValue(number, value.annotations)
                    else -> {
                        val clazz = number?.javaClass?.simpleName ?: "NOT AVAILABLE"
                        println("Class = $clazz")
                        throw TypeCheckException()
                    }
                }
            }
            is NumericType -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Decimal -> decimalValue(number, value.annotations).toDecimal()
                    else -> throw TypeCheckException()
                }
            }
            is TypeReal -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Double -> float64Value(number, value.annotations).toFloat32()
                    else -> throw TypeCheckException()
                }
            }
            is TypeDoublePrecision -> {
                val stringValue = value.value ?: return int16Value(null, value.annotations)
                when (val number = getNumberValueFromString(stringValue)) {
                    is Double -> float64Value(number, value.annotations).toFloat32()
                    else -> throw TypeCheckException()
                }
            }
            is CharType -> TODO("Char value implementation is wrong")
            is CharVarType, is CharVarUnboundedType -> stringValue(value.value, value.annotations)
            else -> error("Cannot perform cast from text to $t")
        }
    }

    // TODO: Fix NULL Collection
    @OptIn(PartiQLValueExperimental::class)
    private fun castFromCollection(value: CollectionValue<*>, t: PartiQLType): PartiQLValue {
        val elements = mutableListOf<PartiQLValue>()
        value.iterator().forEachRemaining {
            elements.add(it)
        }
        return when (t) {
            is ArrayType -> listValue(elements)
            is BagType -> bagValue(elements)
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
