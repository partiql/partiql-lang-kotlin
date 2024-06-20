// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.boolValue

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_NULL__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_null",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY)),
        isNullable = false,
        isNullCall = false,
        isMissable = false,
        isMissingCall = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(args[0].isNull)
    }
}
