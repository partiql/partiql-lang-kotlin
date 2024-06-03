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
        return this.type == PartiQLValueType.BOOL && !this.isNull && this.boolean
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
            PartiQLValueType.STRING, PartiQLValueType.SYMBOL, PartiQLValueType.CHAR -> this.string
            else -> throw TypeCheckException("Expected text, but received ${this.type}.")
        }
    }

    /**
     * Takes in a [PQLValue] that is any integer type ([PartiQLValueType.INT8], [PartiQLValueType.INT8],
     * [PartiQLValueType.INT8], [PartiQLValueType.INT8], [PartiQLValueType.INT8]) and returns the [BigInteger] (potentially
     * coerced) that represents the integer.
     *
     * INTERNAL NOTE: The PLANNER should be handling the coercion. This function should not be necessary.
     *
     * @throws NullPointerException if the value is null
     * @throws TypeCheckException if type is not an integer type
     */
    @OptIn(PartiQLValueExperimental::class)
    fun PQLValue.getBigIntCoerced(): BigInteger {
        return when (this.type) {
            PartiQLValueType.INT8 -> this.byte.toInt().toBigInteger()
            PartiQLValueType.INT16 -> this.short.toInt().toBigInteger()
            PartiQLValueType.INT32 -> this.int.toBigInteger()
            PartiQLValueType.INT64 -> this.long.toBigInteger()
            PartiQLValueType.INT -> this.bigInteger
            else -> throw TypeCheckException()
        }
    }

    /**
     * Takes in a [PQLValue] that is any integer type ([PartiQLValueType.INT8], [PartiQLValueType.INT8],
     * [PartiQLValueType.INT8], [PartiQLValueType.INT8], [PartiQLValueType.INT8]) and returns the [Int] (potentially
     * coerced) that represents the integer.
     *
     * INTERNAL NOTE: This should NOT exist. The PLANNER should be in charge of making sure that the appropriate type is
     * present.
     *
     * @throws NullPointerException if the value is null
     * @throws TypeCheckException if type is not an integer type
     */
    @OptIn(PartiQLValueExperimental::class)
    fun PQLValue.getInt32Coerced(): Int {
        return when (this.type) {
            PartiQLValueType.INT8 -> this.byte.toInt()
            PartiQLValueType.INT16 -> this.short.toInt()
            PartiQLValueType.INT32 -> this.int
            PartiQLValueType.INT64 -> this.long.toInt()
            PartiQLValueType.INT -> this.bigInteger.toInt()
            else -> throw TypeCheckException()
        }
    }
}
