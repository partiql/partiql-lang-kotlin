// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DATE

@OptIn(PartiQLValueExperimental::class)
internal object Fn_CURRENT_DATE____DATE : Fn {

    override val signature = FnSignature(
        name = "current_date",
        returns = DATE,
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function current_date not implemented")
    }
}
