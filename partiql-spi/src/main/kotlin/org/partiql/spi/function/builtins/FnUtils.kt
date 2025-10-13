package org.partiql.spi.function.builtins

import org.partiql.spi.types.PType

internal object FnUtils {
    const val MAXLENGTH = Int.MAX_VALUE

    /**
     * Checks if adding two integers would cause overflow.
     * Uses the property: arg1 >= 0 && arg2 >= 0 && arg1 + arg2 < 0 => overflow occurred
     */
    fun checkLengthOverflow(length1: Int, length2: Int) {
        if (length1 >= 0 && length2 >= 0 && length1 + length2 < 0) {
            throw IllegalArgumentException("String length overflow: $length1 + $length2 exceeds maximum allowed length ($MAXLENGTH)")
        }
    }

    /**
     * Safely adds two lengths and returns the result, throwing if overflow occurs.
     */
    fun addLengths(length1: Int, length2: Int): Int {
        checkLengthOverflow(length1, length2)
        return length1 + length2
    }

    /**
     * Gets the length of a type, handling STRING types that don't have length constraints.
     */
    fun getTypeLength(type: PType): Int {
        return when (type.code()) {
            PType.STRING -> error("STRING type does not have length constraints")
            else -> type.length
        }
    }

    /**
     * Returns the type with higher coercibility for string type coercion.
     * Coercibility order: STRING > CLOB > VARCHAR > CHAR
     */
    fun getHigherCoercibilityType(type1: Int, type2: Int): Int {
        val coercibility = mapOf(
            PType.STRING to 4,
            PType.CLOB to 3,
            PType.VARCHAR to 2,
            PType.CHAR to 1
        )
        val coer1 = coercibility[type1] ?: error("Unknown type: $type1")
        val coer2 = coercibility[type2] ?: error("Unknown type: $type2")
        return if (coer1 >= coer2) type1 else type2
    }
}
