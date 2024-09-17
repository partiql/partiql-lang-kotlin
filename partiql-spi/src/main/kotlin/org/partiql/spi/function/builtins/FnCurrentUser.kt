// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.types.PType

internal object Fn_CURRENT_USER____STRING : Function {

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
