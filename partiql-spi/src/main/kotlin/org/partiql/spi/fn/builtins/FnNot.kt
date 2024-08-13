// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_NOT__BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "not",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.bool())),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
        isMissingCall = true,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].boolean
        return Datum.bool(value.not())
    }
}
