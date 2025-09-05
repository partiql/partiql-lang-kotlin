package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
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
    override fun getUnknownInstance(): Fn? {
        return getDoubleInstance(PType.doublePrecision(), PType.doublePrecision())
    }
}
