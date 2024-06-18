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


internal object Fn_IS_NUMERIC__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_int",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return when (args[0]) {
            is Int8Value,
            is Int16Value,
            is Int32Value,
            is Int64Value,
            is IntValue,
            -> boolValue(true)
            else -> boolValue(false)
        }
    }
}
