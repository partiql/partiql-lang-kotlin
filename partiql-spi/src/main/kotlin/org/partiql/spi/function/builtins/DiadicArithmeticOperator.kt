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
    override fun fillUnknownTable() {
        // Only register unknown with PTypes which arithmetic operator support,
        // otherwise it will break type mismatch check for unsupported types e.g.
        // mod('string', null) should give type mismatched error instead of returning null.
        val allowPTypes = SqlTypeFamily.NUMBER.members +
            SqlTypeFamily.DATETIME.members +
            SqlTypeFamily.INTERVAL.members

        allowPTypes.forEach {
            fillTable(PType.UNKNOWN, it) { lhs, rhs -> getUnknownPTypeInstance(lhs, rhs) }
            fillTable(it, PType.UNKNOWN,) { lhs, rhs -> getPTypeUnknownInstance(lhs, rhs) }
        }
    }

    override fun getUnknownInstance(): Fn? {
        return getDoubleInstance(PType.doublePrecision(), PType.doublePrecision())
    }
}
