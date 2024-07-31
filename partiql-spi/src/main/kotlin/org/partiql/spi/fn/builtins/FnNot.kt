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
internal object Fn_NOT__BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "not",
        returns = BOOL,
        parameters = listOf(FnParameter("value", BOOL)),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
        isMissingCall = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<BoolValue>().value!!
        return boolValue(value.not())
    }
}
