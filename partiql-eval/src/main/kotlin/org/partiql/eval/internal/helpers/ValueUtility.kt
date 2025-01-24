package org.partiql.eval.internal.helpers

import org.partiql.eval.internal.operator.rex.CastTable
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigInteger

/**
 * Holds helper functions for [Datum].
 */
internal object ValueUtility {

    /**
     * @return whether the value is a boolean and the value itself is not-null and true.
     */
    @JvmStatic
    fun Datum.isTrue(): Boolean {
        if (this.isNull || this.isMissing) {
            return false
        }
        return when (this.type.code()) {
            PType.VARIANT -> this.lower().isTrue()
            PType.BOOL -> this.boolean
            else -> false
        }
    }

    /**
     * Asserts that [this] is of a specific type. Note that, if [this] value is null ([Datum.isNull]), then the null
     * value is coerced to the expected type.
     * @throws org.partiql.spi.errors.PRuntimeException when the input value is a non-null value of the wrong type.
     * @return a [Datum] corresponding to the expected type; this will either be the input value if the value is
     * already of the expected type, or it will be a null value of the expected type.
     */
    fun Datum.check(type: PType): Datum {
        if (this.type == type) {
            return this
        }
        if (this.type.code() == PType.VARIANT) {
            return this.lower().check(type)
        }
        if (!this.isNull) {
            throw PErrors.unexpectedTypeException(this.type, listOf(type))
        }
        return Datum.nullValue(type)
    }

    /**
     * Specifically checks for struct, or coerce rows to structs. Same functionality as [check].
     */
    fun Datum.checkStruct(): Datum {
        if (this.type.code() == PType.VARIANT) {
            return this.lower().checkStruct()
        }
        if (this.type.code() == PType.STRUCT) {
            return this
        }
        if (this.type.code() == PType.ROW) {
            return CastTable.cast(this, PType.struct())
        }
        return this.check(PType.struct())
    }

    /**
     * Returns the underlying string value of a PartiQL text value
     *
     * @throws NullPointerException if the value is null
     * @throws org.partiql.spi.errors.PRuntimeException if the value's type is not a text type (string, symbol, char)
     */
    fun Datum.getText(): String {
        return when (this.type.code()) {
            PType.VARIANT -> this.lower().getText()
            PType.STRING, PType.CHAR -> this.string
            else -> {
                throw PErrors.unexpectedTypeException(this.type, listOf(PType.string(), PType.character()))
            }
        }
    }

    /**
     * Converts all number values to [BigInteger]. If the number is [PType.DECIMAL] or [PType.NUMERIC], this asserts that
     * the scale is zero.
     *
     * INTERNAL NOTE: The PLANNER should be handling the coercion. This function should not be necessary.
     *
     * TODO: This is used specifically for LIMIT and OFFSET. This makes the conformance tests pass by coercing values
     *  of type [PType.NUMERIC] and [PType.DECIMAL], but this is unspecified. Do we allow for LIMIT 2.0? Or of
     *  a value that is greater than [PType.BIGINT]'s MAX value by using a [PType.DECIMAL] with a high precision and scale
     *  of zero? This hasn't been decided, however, as the conformance tests allow for this, this function coerces
     *  the value to a [BigInteger] regardless of the number's type.
     *
     * @throws NullPointerException if the value is null
     * @throws org.partiql.spi.errors.PRuntimeException if type is not an integer type
     */
    fun Datum.getBigIntCoerced(): BigInteger {
        return when (this.type.code()) {
            PType.VARIANT -> this.lower().getBigIntCoerced()
            PType.TINYINT -> this.byte.toInt().toBigInteger()
            PType.SMALLINT -> this.short.toInt().toBigInteger()
            PType.INTEGER -> this.int.toBigInteger()
            PType.BIGINT -> this.long.toBigInteger()
            PType.NUMERIC, PType.DECIMAL -> {
                val decimal = this.bigDecimal
                if (decimal.scale() != 0) {
                    throw PErrors.unexpectedTypeException(this.type, listOf(PType.tinyint(), PType.smallint(), PType.integer(), PType.bigint()))
                }
                return decimal.toBigInteger()
            }
            else -> {
                throw PErrors.unexpectedTypeException(this.type, listOf(PType.tinyint(), PType.smallint(), PType.integer(), PType.bigint()))
            }
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
     * @throws org.partiql.spi.errors.PRuntimeException if type is not an integer type
     */
    fun Datum.getInt32Coerced(): Int {
        return when (this.type.code()) {
            PType.VARIANT -> this.lower().getInt32Coerced()
            PType.TINYINT -> this.byte.toInt()
            PType.SMALLINT -> this.short.toInt()
            PType.INTEGER -> this.int
            PType.BIGINT -> this.long.toInt()
            else -> {
                throw PErrors.unexpectedTypeException(this.type, listOf(PType.tinyint(), PType.smallint(), PType.integer(), PType.bigint()))
            }
        }
    }
}
