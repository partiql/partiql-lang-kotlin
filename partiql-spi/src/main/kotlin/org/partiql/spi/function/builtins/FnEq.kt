// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

// Memoize the comparator.
private val comparator = Datum.comparator()

/**
 * According to SQL:1999:
 * > If either XV or YV is the null value, then `X <comp op> Y` is unknown
 *
 * According to the PartiQL Specification:
 * > Equality never fails in the type-checking mode and never returns MISSING in the permissive mode. Instead, it can
 * compare values of any two types, according to the rules of the PartiQL type system. For example, 5 = 'a' is false.
 *
 * For the existing conformance tests, whenever an operand is NULL or MISSING, the output is NULL. This implementation
 * follows this.
 *
 * TODO: The PartiQL Specification needs to clearly define the semantics of MISSING. That being said, this implementation
 *  follows the existing conformance tests and SQL:1999.
 */
internal val Fn_EQ__ANY_ANY__BOOL = Function.static(
    name = "eq",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.dynamic()),
        Parameter("rhs", PType.dynamic()),
    ),
    isNullCall = true,
    isMissingCall = false,
) { args ->
    val lhs = args[0]
    val rhs = args[1]
    if (lhs.isMissing || rhs.isMissing) {
        return@static Datum.nullValue(PType.bool())
    }
    Datum.bool(comparator.compare(lhs, rhs) == 0)
}
