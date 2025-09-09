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
        // We should only register unknown with PTypes combination, which arithmetic operator supports.
        // And for unregistered combinations, it falls into the data type mismatch scenario which should give
        // the data type mismatch error in strict mode and return MISSING in permissive mode,
        // e.g. mod('string', null) which is not registered and is a type mismatch
        // and mod(3, null) is registered returns null
        val allowPTypes = SqlTypeFamily.NUMBER.members +
            SqlTypeFamily.DATETIME.members +
            SqlTypeFamily.INTERVAL.members

        allowPTypes.forEach {
            fillTable(PType.UNKNOWN, it) { lhs, rhs -> getUnknownPTypeInstance(lhs, rhs) }
            fillTable(it, PType.UNKNOWN,) { lhs, rhs -> getPTypeUnknownInstance(lhs, rhs) }
        }
    }

    override fun getUnknownPTypeInstance(lhs: PType, rhs: PType): Fn? {
        return basic(rhs, lhs, rhs) { args -> throw NotImplementedError() }
    }

    override fun getPTypeUnknownInstance(lhs: PType, rhs: PType): Fn? {
        return basic(lhs, lhs, rhs) { args -> throw NotImplementedError() }
    }

    override fun getUnknownInstance(): Fn? {
        return getDoubleInstance(PType.doublePrecision(), PType.doublePrecision())
    }
}
