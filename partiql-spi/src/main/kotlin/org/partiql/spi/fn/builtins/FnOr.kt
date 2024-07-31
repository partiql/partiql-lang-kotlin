// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class)
internal object Fn_OR__BOOL_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", BOOL),
        ),
        isNullable = true,
        isNullCall = false,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BoolValue>().value
        val rhs = args[1].check<BoolValue>().value
        val toReturn = when {
            lhs == true || rhs == true -> true
            lhs == null || rhs == null -> null
            else -> false
        }
        return boolValue(toReturn)
    }
}
