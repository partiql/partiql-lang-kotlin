// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BagValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.boolValue

@OptIn(PartiQLValueExperimental::class)
internal object Fn_IS_BAG__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_bag",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(args[0] is BagValue<*>)
    }
}
