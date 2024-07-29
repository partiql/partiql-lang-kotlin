// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class)
internal object Fn_AND__BOOL_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "and",
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
        val lhs = args[0].check<BoolValue>()
        val rhs = args[1].check<BoolValue>()
        // SQL:1999 Section 6.30 Table 13
        val toReturn = when {
            lhs.isNull && rhs.isNull -> null
            lhs.value == true && rhs.isNull -> null
            rhs.value == true && lhs.isNull -> null
            lhs.value == false || rhs.value == false -> false
            else -> true
        }
        return boolValue(toReturn)
    }
}
