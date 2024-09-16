// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_CURRENT_USER____STRING : Fn {

    override val signature = FnSignature(
        name = "current_user",
        returns = PType.string(),
        parameters = listOf(),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function current_user not implemented")
    }
}
