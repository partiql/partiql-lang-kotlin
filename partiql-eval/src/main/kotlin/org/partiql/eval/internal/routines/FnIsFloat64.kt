// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.boolValue


internal object Fn_IS_FLOAT64__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_float64",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return when (args[0]) {
            is Float32Value,
            is Float64Value,
            -> boolValue(true)
            else -> boolValue(false)
        }
    }
}
