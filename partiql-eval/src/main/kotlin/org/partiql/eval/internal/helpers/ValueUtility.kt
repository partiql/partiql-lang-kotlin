package org.partiql.eval.internal.helpers

import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import java.math.BigInteger

/**
 * Holds helper functions for [PartiQLValue].
 */
internal object ValueUtility {

    /**
     * @return whether the value is a boolean and the value itself is not-null and true.
     */
    @OptIn(PartiQLValueExperimental::class)
    @JvmStatic
    fun PQLValue.isTrue(): Boolean {
        return this.type == PartiQLValueType.BOOL && !this.isNull && this.boolValue
    }

    /**
     * Asserts that [this] is of a specific type. Note that, if [this] value is null ([PQLValue.isNull]), then the null
     * value is coerced to the expected type.
     * @throws TypeCheckException when the input value is a non-null value of the wrong type.
     * @return a [PQLValue] corresponding to the expected type; this will either be the input value if the value is
     * already of the expected type, or it will be a null value of the expected type.
     */
    @OptIn(PartiQLValueExperimental::class)
    fun PQLValue.check(type: PartiQLValueType): PQLValue {
        if (this.type == type) {
            return this
        }
        if (!this.isNull) {
            throw TypeCheckException("Expected type $type but received ${this.type}.")
        }
        return PQLValue.nullValue(type)
    }

    /**
     * Returns the underlying string value of a PartiQL text value
     *
     * @throws NullPointerException if the value is null
     * @throws TypeCheckException if the value's type is not a text type (string, symbol, char)
     */
    @OptIn(PartiQLValueExperimental::class)
    fun PQLValue.getText(): String {
        return when (this.type) {
            PartiQLValueType.STRING -> this.stringValue
            PartiQLValueType.SYMBOL -> this.symbolValue
            PartiQLValueType.CHAR -> this.charValue
            else -> throw TypeCheckException("Expected text, but received ${this.type}.")
        }
    }

    /**
     * Takes in a [PQLValue] that is any integer type ([PartiQLValueType.INT8], [PartiQLValueType.INT8],
     * [PartiQLValueType.INT8], [PartiQLValueType.INT8], [PartiQLValueType.INT8]) and returns the [BigInteger] (potentially
     * coerced) that represents the integer.
     *
     * @throws NullPointerException if the value is null
     * @throws TypeCheckException if type is not an integer type
     */
    @OptIn(PartiQLValueExperimental::class)
    fun PQLValue.getBigIntCoerced(): BigInteger {
        return when (this.type) {
            PartiQLValueType.INT8 -> this.int8Value.toInt().toBigInteger()
            PartiQLValueType.INT16 -> this.int16Value.toInt().toBigInteger()
            PartiQLValueType.INT32 -> this.int32Value.toBigInteger()
            PartiQLValueType.INT64 -> this.int64Value.toBigInteger()
            PartiQLValueType.INT -> this.intValue
            else -> throw TypeCheckException()
        }
    }

    /**
     * Takes in a [PQLValue] that is any integer type ([PartiQLValueType.INT8], [PartiQLValueType.INT8],
     * [PartiQLValueType.INT8], [PartiQLValueType.INT8], [PartiQLValueType.INT8]) and returns the [Int] (potentially
     * coerced) that represents the integer.
     *
     * @throws NullPointerException if the value is null
     * @throws TypeCheckException if type is not an integer type
     */
    @OptIn(PartiQLValueExperimental::class)
    fun PQLValue.getInt32Coerced(): Int {
        return when (this.type) {
            PartiQLValueType.INT8 -> this.int8Value.toInt()
            PartiQLValueType.INT16 -> this.int16Value.toInt()
            PartiQLValueType.INT32 -> this.int32Value
            PartiQLValueType.INT64 -> this.int64Value.toInt()
            PartiQLValueType.INT -> this.intValue.toInt()
            else -> throw TypeCheckException()
        }
    }
}
