// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_IS_NULL__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_null",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.dynamic())),
        isNullable = false,
        isNullCall = false,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        if (args[0].isMissing) {
            return Datum.bool(true)
        }
        return Datum.bool(args[0].isNull)
    }
}
