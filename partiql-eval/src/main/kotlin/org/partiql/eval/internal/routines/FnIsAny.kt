// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BOOL


internal object Fn_IS_DYNAMIC__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_any",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function is_any not implemented")
    }
}
