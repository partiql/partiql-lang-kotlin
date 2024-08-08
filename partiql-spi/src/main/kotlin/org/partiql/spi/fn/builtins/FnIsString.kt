// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.errors.TypeCheckException
import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_IS_STRING__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_string",
        returns = PType.typeBool(),
        parameters = listOf(FnParameter("value", PType.typeDynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return Datum.bool(args[0].type.kind == PType.Kind.STRING)
    }
}

internal object Fn_IS_STRING__INT32_ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_string",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("type_parameter_1", PType.typeInt()),
            FnParameter("value", PType.typeDynamic()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[1]
        if (v.type.kind != PType.Kind.STRING) {
            return Datum.bool(false)
        }
        val length = args[0].int
        if (length < 0) {
            throw TypeCheckException()
        }
        return Datum.bool(v.string.length <= length)
    }
}
