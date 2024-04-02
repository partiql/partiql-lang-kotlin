package org.partiql.spi.connector.sql.builtins.internal

import org.partiql.value.NumericType
import kotlin.math.max

internal object FnUtils {
    /**
     * Returns an [Iterator] containing all possible [NumericType]'s. This does not return
     * [org.partiql.value.TypeNumericUnbounded]. This is useful for the following scenario
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
        }
    }

    internal class DiadicFunction<T>(
        val lhs: T,
        val rhs: T,
        val result: T
    )

    /**
     * According to SQL:1999 Section 6.26:
     * Let S1 and S2 be the scale of the first and second operands respectively. The precision of the result of
     * multiplication is implementation-defined, and the scale is S1 + S2.
     *
     * This does not return [org.partiql.value.TypeNumericUnbounded].
     *
     * For this implementation, the resulting precision is always [NumericType.MAX_PRECISION], however, this is subject
     * to change.
     */
    internal fun numericMultiplicationTypes(): Iterator<DiadicFunction<NumericType>> {
        return iterator {
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

    /**
     * According to SQL:1999 Section 6.26:
     * Let S1 and S2 be the scale of the first and second operands respectively. The precision of the result of
     * addition and subtraction is implementation-defined, and the scale is the maximum of S1 and S2.
     *
     * This does not return [org.partiql.value.TypeNumericUnbounded].
     *
     * For this implementation, the resulting precision is always [NumericType.MAX_PRECISION], however, this is subject
     * to change.
     */
    internal fun numericAdditionSubtractionTypes(): Iterator<DiadicFunction<NumericType>> {
        return iterator {
            for (scaleLhs in 1..NumericType.MAX_PRECISION) {
                for (scaleRhs in 1..NumericType.MAX_PRECISION) {
                    val scaleResult = max(scaleLhs, scaleRhs)
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

    /**
     * According to SQL:1999 Section 6.26:
     * Let S1 and S2 be the scale of the first and second operands respectively. The precision and scale of the result
     * of division is implementation-defined.
     *
     * This does not return [org.partiql.value.TypeNumericUnbounded].
     *
     * For this implementation, it follows SQL Server's result precision and scale:
     * Result Precision = p1 - s1 + s2 + max(6, s1 + p2 + 1)
     * Result Scale = max(6, s1 + p2 + 1)
     */
    internal fun numericDivisionTypes(): Iterator<DiadicFunction<NumericType>> {
        return iterator {
            for (lhs in numericTypes()) {
                for (rhs in numericTypes()) {
                    val resultPrecision = lhs.precision - lhs.scale + rhs.scale + max(6, lhs.scale + rhs.precision + 1)
                    val resultScale = max(6, lhs.scale + rhs.precision + 1)
                    if (resultPrecision <= NumericType.MAX_PRECISION && resultScale <= resultPrecision) {
                        yield(DiadicFunction(lhs, rhs, NumericType(resultPrecision, resultScale)))
                    }
                }
            }
        }
    }
}
