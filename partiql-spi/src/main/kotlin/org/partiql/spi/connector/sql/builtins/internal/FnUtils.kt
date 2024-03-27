package org.partiql.spi.connector.sql.builtins.internal

import org.partiql.value.NumericType
import org.partiql.value.PartiQLType
import org.partiql.value.TypeIntBig
import org.partiql.value.TypeNumericUnbounded

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
    internal fun numericTypes(): Iterator<PartiQLType> {
        return iterator {
            // Bounded Numeric Types
            for (p in 1..NumericType.MAX_PRECISION) {
                for (s in 1..p) {
                    yield(NumericType(p, s))
                }
            }
            // Unbounded Numeric
            yield(TypeNumericUnbounded)
            // Unbounded BigInt
            yield(TypeIntBig)
        }
    }

    class DiadicFunction(
        val lhs: NumericType,
        val rhs: NumericType,
        val result: NumericType
    )

    internal fun numericMultiplicationTypes(): Iterator<DiadicFunction> {
        return iterator {
            // Bounded Numeric Types
            for (scaleLhs in 1..NumericType.MAX_PRECISION) {
                for (scaleRhs in 1..NumericType.MAX_PRECISION) {
                    val scaleResult = scaleLhs + scaleRhs
                    if (scaleResult <= NumericType.MAX_PRECISION) {
                        yield(
                            DiadicFunction(
                                lhs = NumericType(NumericType.MAX_PRECISION, scaleLhs),
                                rhs = NumericType(NumericType.MAX_PRECISION, scaleRhs),
                                result = NumericType(NumericType.MAX_PRECISION, scaleResult),
                            )
                        )
                    }
                }
            }
        }
    }
}
