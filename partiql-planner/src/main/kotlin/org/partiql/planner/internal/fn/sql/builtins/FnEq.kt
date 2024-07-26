// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.boolValue

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
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__ANY_ANY__BOOL : Fn {

    private val comparator = PartiQLValue.comparator()

    override val signature = FnSignature(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", ANY),
            FnParameter("rhs", ANY),
        ),
        isNullable = true,
        isNullCall = true,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0]
        val rhs = args[1]
        if (lhs.type == MISSING || rhs.type == MISSING) {
            return boolValue(null)
        }
        return boolValue(comparator.compare(lhs, rhs) == 0)
    }
}
