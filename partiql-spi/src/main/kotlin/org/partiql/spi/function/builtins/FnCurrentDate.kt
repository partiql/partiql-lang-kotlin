// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.types.PType

internal object Fn_CURRENT_DATE____DATE : Function {

    override val signature = FnSignature(
        name = "current_date",
        returns = PType.date(),
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function current_date not implemented")
    }
}
