// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

/**
 * According to SQL:1999:
 * > If either XV or YV is the null value, then `X <comp op> Y` is unknown
 *
 * According to the PartiQL Specification:
 * > Equality never fails in the type-checking mode and never returns MISSING in the permissive mode. Instead, it can
 * compare values of any two types, according to the rules of the PartiQL type system. For example, 5 = 'a' is false.
 *
 * For the existing conformance tests, when an operand is NULL, the output is NULL. When an operand is MISSING,
 * the output is MISSING (missing value propagation). This implementation follows the existing conformance tests.
 *
 * TODO: The PartiQL Specification needs to clearly define the semantics of MISSING. That being said, this implementation
 *  follows the existing conformance tests and SQL:1999.
 */
private val name = FunctionUtils.hide("eq")
internal val FnEq = FnOverload.Builder(name)
    .addParameters(PType.dynamic(), PType.dynamic())
    .returns(PType.bool())
    .isNullCall(false)
    .isMissingCall(false)
    .body { args ->
        val lhs = args[0]
        val rhs = args[1]
        if (lhs.isMissing || rhs.isMissing) {
            Datum.missing(PType.bool())
        } else if (lhs.isNull || rhs.isNull) {
            Datum.nullValue(PType.bool())
        } else {
            Datum.bool(Datum.comparator().compare(lhs, rhs) == 0)
        }
    }
    .build()
