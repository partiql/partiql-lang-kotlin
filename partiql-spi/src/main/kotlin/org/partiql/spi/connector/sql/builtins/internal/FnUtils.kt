package org.partiql.spi.connector.sql.builtins.internal

import org.partiql.value.NumericType

internal object FnUtils {
    /**
     * Returns an [Iterator] containing all possible [NumericType]'s. This is useful for the following scenario
     * described by SQL:1999:
     *
     * CREATE FUNCTION "ABS" (
     *      N NUMERIC ( P, S )
     * ) RETURNS NUMERIC ( P, S )
     * SPECIFIC ABSNUMERICP_S
     * RETURN ABS ( N ) ;
     *
     * Let P assume all character string values that are the minimal literal for an exact numeric value
     * of scale 0 (zero) between 1 (one) and MP, let S assume all character string values that are
     * the minimal literal for an exact numeric value of scale 0 (zero) between 1 (one) and P
     */
    internal fun numericTypes(): Iterator<NumericType> {
        return iterator {
            // Bounded Numeric Types
            for (p in 1..NumericType.MAX_PRECISION) {
                for (s in 1..p) {
                    yield(NumericType(p, s))
                }
            }
            // Unbounded Numeric
            yield(NumericType(null, null))
            // Unbounded BigInt
            yield(NumericType(null, 0))
        }
    }
}