// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.boolValue


internal object Fn_IS_TINYINT__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_int8",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return when (val arg = args[0]) {
            is Int8Value -> boolValue(true)
            is Int16Value -> {
                val v = arg.value!!
                boolValue(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            is Int32Value -> {
                val v = arg.value!!
                boolValue(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            is Int64Value -> {
                val v = arg.value!!
                boolValue(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            is IntValue -> {
                val v = arg.value!!
                return try {
                    v.byteValueExact()
                    boolValue(true)
                } catch (_: ArithmeticException) {
                    boolValue(false)
                }
            }
            else -> boolValue(false)
        }
    }
}
