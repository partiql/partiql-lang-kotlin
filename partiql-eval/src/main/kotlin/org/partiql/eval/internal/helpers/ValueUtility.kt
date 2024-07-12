package org.partiql.eval.internal.helpers

import org.partiql.errors.TypeCheckException
import org.partiql.eval.value.Datum
import org.partiql.types.PType
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
    @JvmStatic
    fun Datum.isTrue(): Boolean {
        return this.type.kind == PType.Kind.BOOL && !this.isNull && this.boolean
    }

    @OptIn(PartiQLValueExperimental::class)
    fun Datum.check(type: PartiQLValueType): Datum {
        return this.check(PType.fromPartiQLValueType(type))
    }

    /**
     * Asserts that [this] is of a specific type. Note that, if [this] value is null ([Datum.isNull]), then the null
     * value is coerced to the expected type.
     * @throws TypeCheckException when the input value is a non-null value of the wrong type.
     * @return a [Datum] corresponding to the expected type; this will either be the input value if the value is
     * already of the expected type, or it will be a null value of the expected type.
     */
    fun Datum.check(type: PType): Datum {
        if (this.type == type) {
            return this
        }
        if (!this.isNull) {
            throw TypeCheckException("Expected type $type but received ${this.type}.")
        }
        return Datum.nullValue(type)
    }

    /**
     * Returns the underlying string value of a PartiQL text value
     *
     * @throws NullPointerException if the value is null
     * @throws TypeCheckException if the value's type is not a text type (string, symbol, char)
     */
    fun Datum.getText(): String {
        return when (this.type.kind) {
            PType.Kind.STRING, PType.Kind.SYMBOL, PType.Kind.CHAR -> this.string
            else -> throw TypeCheckException("Expected text, but received ${this.type}.")
        }
    }

    /**
     * Takes in a [Datum] that is any integer type ([PartiQLValueType.INT8], [PartiQLValueType.INT8],
     * [PartiQLValueType.INT8], [PartiQLValueType.INT8], [PartiQLValueType.INT8]) and returns the [BigInteger] (potentially
     * coerced) that represents the integer.
     *
     * INTERNAL NOTE: The PLANNER should be handling the coercion. This function should not be necessary.
     *
     * @throws NullPointerException if the value is null
     * @throws TypeCheckException if type is not an integer type
     */
    fun Datum.getBigIntCoerced(): BigInteger {
        return when (this.type.kind) {
            PType.Kind.TINYINT -> this.byte.toInt().toBigInteger()
            PType.Kind.SMALLINT -> this.short.toInt().toBigInteger()
            PType.Kind.INT -> this.int.toBigInteger()
            PType.Kind.BIGINT -> this.long.toBigInteger()
            PType.Kind.INT_ARBITRARY -> this.bigInteger
            else -> throw TypeCheckException()
        }
    }

    /**
     * Takes in a [Datum] that is any integer type ([PartiQLValueType.INT8], [PartiQLValueType.INT8],
     * [PartiQLValueType.INT8], [PartiQLValueType.INT8], [PartiQLValueType.INT8]) and returns the [Int] (potentially
     * coerced) that represents the integer.
     *
     * INTERNAL NOTE: This should NOT exist. The PLANNER should be in charge of making sure that the appropriate type is
     * present.
     *
     * @throws NullPointerException if the value is null
     * @throws TypeCheckException if type is not an integer type
     */
    fun Datum.getInt32Coerced(): Int {
        return when (this.type.kind) {
            PType.Kind.TINYINT -> this.byte.toInt()
            PType.Kind.SMALLINT -> this.short.toInt()
            PType.Kind.INT -> this.int
            PType.Kind.BIGINT -> this.long.toInt()
            PType.Kind.INT_ARBITRARY -> this.bigInteger.toInt()
            else -> throw TypeCheckException()
        }
    }
}
