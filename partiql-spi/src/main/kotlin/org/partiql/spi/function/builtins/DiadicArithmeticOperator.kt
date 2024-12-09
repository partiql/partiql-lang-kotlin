package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter

/**
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 */
internal abstract class DiadicArithmeticOperator(name: String) : DiadicOperator(
    name,
    Parameter.number("lhs"),
    Parameter.number("rhs")
)
