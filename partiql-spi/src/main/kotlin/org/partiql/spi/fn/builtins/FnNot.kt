// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_NOT__BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "not",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.bool())),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg = args[0]
        if (arg.isMissing) {
            return Datum.nullValue(PType.bool())
        }
        val value = arg.boolean
        return Datum.bool(value.not())
    }
}
