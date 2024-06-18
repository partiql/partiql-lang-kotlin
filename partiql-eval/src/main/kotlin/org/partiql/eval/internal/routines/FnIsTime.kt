// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.PType.Kind.INT
import org.partiql.value.TimeValue
import org.partiql.value.boolValue


internal object Fn_IS_TIME__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_time",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return boolValue(args[0] is TimeValue)
    }
}


internal object Fn_IS_TIME__BOOL_INT_DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_time",
        returns = BOOL,
        parameters = listOf(
            FnParameter("type_parameter_1", BOOL),
            FnParameter("type_parameter_2", INT),
            FnParameter("value", DYNAMIC),
        ),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function is_time not implemented")
    }
}
