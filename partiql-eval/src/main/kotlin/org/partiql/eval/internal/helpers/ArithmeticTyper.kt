package org.partiql.eval.internal.helpers

import org.partiql.types.PType
import org.partiql.types.PType.Kind
import kotlin.math.max
import kotlin.math.min

/**
 * This is a mirror copy to [org.partiql.planner.internal.typer.ArithmeticTyper].
 */
internal object ArithmeticTyper {

    /**
     * Follows SQL-Server for decimals:
     * Result Precision = max(s1, s2) + max(p1 - s1, p2 - s2) + 1
     * Result Scale = max(s1, s2)
     * @return null if the operation is not allowed on the input types.
     */
    internal fun add(lhs: PType, rhs: PType): PType? = arithmeticBinary(lhs, rhs) { lDec, rDec ->
        val precision = max(lDec.scale, rDec.scale) + max(lDec.precision - lDec.scale, rDec.precision - rDec.scale) + 1
        val scale = max(lDec.scale, rDec.scale)
        precision to scale
    }

    /**
     * Follows SQL-Server for decimals:
     * Result Precision = max(s1, s2) + max(p1 - s1, p2 - s2) + 1
     * Result Scale = max(s1, s2)
     * @return null if the operation is not allowed on the input types.
     */
    internal fun subtract(lhs: PType, rhs: PType): PType? = arithmeticBinary(lhs, rhs) { lDec, rDec ->
        val precision = max(lhs.scale, rhs.scale) + max(lhs.precision - lhs.scale, rhs.precision - rhs.scale) + 1
        val scale = max(lhs.scale, rhs.scale)
        precision to scale
    }

    /**
     * Follows SQL-Server for decimals:
     * Result Precision = p1 - s1 + s2 + max(6, s1 + p2 + 1)
     * Result Scale = max(6, s1 + p2 + 1)
     * @return null if the operation is not allowed on the input types.
     */
    internal fun divide(lhs: PType, rhs: PType): PType? = arithmeticBinary(lhs, rhs) { lDec, rDec ->
        val precision = lhs.precision - rhs.scale + lhs.scale + max(6, lhs.scale + rhs.precision + 1)
        val scale = max(6, lhs.scale + rhs.precision + 1)
        precision to scale
    }

    /**
     * Follows SQL-Server for decimals:
     * Result Precision = p1 + p2 + 1
     * Result Scale = s1 + s2
     * @return null if the operation is not allowed on the input types.
     */
    internal fun multiply(lhs: PType, rhs: PType): PType? = arithmeticBinary(lhs, rhs) { lDec, rDec ->
        val precision = lhs.precision + rhs.precision + 1
        val scale = lhs.scale + rhs.scale
        precision to scale
    }

    /**
     * Follows SQL-Server for decimals:
     * Result Precision: min(p1 - s1, p2 - s2) + max(s1, s2)
     * Result Scale: max(s1, s2)
     * @return null if the operation is not allowed on the input types.
     */
    internal fun modulo(lhs: PType, rhs: PType): PType? = arithmeticBinary(lhs, rhs) { lDec, rDec ->
        val precision = min(lhs.precision - lhs.scale, rhs.precision - rhs.scale) + max(lhs.scale, rhs.scale)
        val scale = max(lhs.scale, rhs.scale)
        precision to scale
    }

    internal fun negative(arg: PType): PType? = arithmeticUnary(arg)

    internal fun positive(arg: PType): PType? = arithmeticUnary(arg)

    private fun arithmeticUnary(arg: PType): PType? {
        val argMayBeNumber = arg.kind == Kind.DYNAMIC || TypeFamily.NUMBERS.contains(arg.kind)
        return when (argMayBeNumber) {
            true -> arg
            false -> null
        }
    }

    private fun arithmeticBinary(
        lhs: PType,
        rhs: PType,
        handleDecimal: (PType, PType) -> Pair<Int, Int>
    ): PType? {
        val lhsCannotBeNumber = lhs.kind != Kind.DYNAMIC && !TypeFamily.NUMBERS.contains(lhs.kind)
        val rhsCannotBeNumber = rhs.kind != Kind.DYNAMIC && !TypeFamily.NUMBERS.contains(rhs.kind)
        if (lhsCannotBeNumber || rhsCannotBeNumber) {
            return null
        }
        if (lhs.kind == Kind.DYNAMIC || rhs.kind == Kind.DYNAMIC) {
            return PType.typeDynamic()
        }
        val lhsPrecedence = TypePrecedence[lhs.kind]!!
        val rhsPrecedence = TypePrecedence[rhs.kind]!!
        val comp = lhsPrecedence.compareTo(rhsPrecedence)
        when (comp) {
            -1 -> return rhs
            1 -> return lhs
            0 -> if (lhs.kind != Kind.DECIMAL) {
                return lhs
            }

            else -> error("This shouldn't have occurred.")
        }
        val (precision, scale) = handleDecimal(lhs, rhs)
        // TODO: Check if this is what we want
        return when (precision > 38 || scale > 38) {
            true -> PType.typeDecimalArbitrary()
            false -> PType.typeDecimal(precision, scale)
        }
    }
}
