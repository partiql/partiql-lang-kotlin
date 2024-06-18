// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.CharValue
import org.partiql.value.Int32Value
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.PType.Kind.INT
import org.partiql.value.StringValue
import org.partiql.value.boolValue
import org.partiql.value.check


internal object Fn_IS_CHAR__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_char",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return boolValue(args[0] is CharValue)
    }
}


internal object Fn_IS_CHAR__INT_DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_char",
        returns = BOOL,
        parameters = listOf(
            FnParameter("type_parameter_1", INT),
            FnParameter("value", DYNAMIC),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        if (value !is StringValue) {
            return boolValue(false)
        }
        val length = args[0].check<Int32Value>().value
        if (length == null || length < 0) {
            throw TypeCheckException()
        }
        return boolValue(value.value!!.length == length)
    }
}
