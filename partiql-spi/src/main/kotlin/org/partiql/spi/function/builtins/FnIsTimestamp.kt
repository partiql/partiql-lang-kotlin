// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_IS_TIMESTAMP__ANY__BOOL : Function {

    override val signature = FnSignature(
        name = "is_timestamp",
        returns = PType.bool(),
        parameters = listOf(Parameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val argKind = args[0].type.kind
        return Datum.bool(argKind == PType.Kind.TIMESTAMPZ || argKind == PType.Kind.TIMESTAMP)
    }
}

internal object Fn_IS_TIMESTAMP__BOOL_INT32_ANY__BOOL : Function {

    override val signature = FnSignature(
        name = "is_timestamp",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("type_parameter_1", PType.bool()),
            Parameter("type_parameter_2", PType.integer()),
            Parameter("value", PType.dynamic()),
        ),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function is_timestamp not implemented")
    }
}
