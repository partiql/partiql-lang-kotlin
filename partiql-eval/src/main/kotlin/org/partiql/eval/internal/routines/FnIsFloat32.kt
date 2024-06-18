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


internal object Fn_IS_FLOAT32__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_float32",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return when (val arg = args[0]) {
            is Float32Value -> boolValue(true)
            is Float64Value -> {
                val v = arg.value!!
                boolValue(Float.MIN_VALUE <= v && v <= Float.MAX_VALUE)
            }
            else -> boolValue(false)
        }
    }
}
