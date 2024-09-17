// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_IS_TIMESTAMP__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_timestamp",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val argKind = args[0].type.kind
        return Datum.bool(argKind == PType.Kind.TIMESTAMPZ || argKind == PType.Kind.TIMESTAMP)
    }
}

internal object Fn_IS_TIMESTAMP__BOOL_INT32_ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_timestamp",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("type_parameter_1", PType.bool()),
            FnParameter("type_parameter_2", PType.integer()),
            FnParameter("value", PType.dynamic()),
        ),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function is_timestamp not implemented")
    }
}
