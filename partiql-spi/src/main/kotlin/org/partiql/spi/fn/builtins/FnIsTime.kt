// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_IS_TIME__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_time",
        returns = PType.typeBool(),
        parameters = listOf(FnParameter("value", PType.typeDynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val argKind = args[0].type.kind
        return Datum.bool(argKind == PType.Kind.TIME_WITH_TZ || argKind == PType.Kind.TIME_WITHOUT_TZ)
    }
}

internal object Fn_IS_TIME__BOOL_INT32_ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_time",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("type_parameter_1", PType.typeBool()),
            FnParameter("type_parameter_2", PType.typeInt()),
            FnParameter("value", PType.typeDynamic()),
        ),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function is_time not implemented")
    }
}
