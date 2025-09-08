package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType

/**
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 * @param hidesName dictates whether the [name] should be hidden; true by default.
 */
internal abstract class DiadicArithmeticOperator(name: String, hidesName: Boolean = true) : DiadicOperator(
    name,
    PType.doublePrecision(),
    PType.doublePrecision(),
    hidesName = hidesName
) {
    override fun getUnknownInstance(lhs: PType, rhs: PType): Fn? {
        // The base getUnknownInstance applies to all PTypes with unknown combinations.
        // Override the base to apply to PTypes, which arithmetic operator supports,
        // otherwise it will break type mismatch check for unsupported types e.g.
        // mod('string', null) should give type
        val allowPTypes = SqlTypeFamily.NUMBER.members +
                SqlTypeFamily.DATETIME.members +
                SqlTypeFamily.INTERVAL.members +
                setOf(PType.UNKNOWN)

        if (lhs.code() in allowPTypes && rhs.code() in allowPTypes) {
            return super.getUnknownInstance(lhs, rhs)
        }

        return null
    }

    override fun getUnknownInstance(): Fn? {
        return getDoubleInstance(PType.doublePrecision(), PType.doublePrecision())
    }
}
