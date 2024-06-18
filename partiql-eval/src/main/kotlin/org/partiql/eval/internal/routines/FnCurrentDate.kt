// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnSignature
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DATE


internal object Fn_CURRENT_DATE____DATE : Routine {

    override val signature = FnSignature(
        name = "current_date",
        returns = DATE,
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function current_date not implemented")
    }
}
