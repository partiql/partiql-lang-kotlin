// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnSignature
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.STRING


internal object Fn_CURRENT_USER____STRING : Routine {

    override val signature = FnSignature(
        name = "current_user",
        returns = STRING,
        parameters = listOf(),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function current_user not implemented")
    }
}
