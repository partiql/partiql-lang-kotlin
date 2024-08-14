// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.errors.TypeCheckException
import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_IS_CHAR__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_char",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return Datum.bool(args[0].type.kind == PType.Kind.CHAR)
    }
}

internal object Fn_IS_CHAR__INT32_ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_char",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("type_parameter_1", PType.integer()),
            FnParameter("value", PType.dynamic()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    private val TEXT_TYPES_WITH_LENGTH = setOf(
        PType.Kind.CHAR,
        PType.Kind.VARCHAR
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        if (value.type.kind in TEXT_TYPES_WITH_LENGTH) {
            return Datum.bool(false)
        }
        val length = args[0].int
        if (length < 0) {
            throw TypeCheckException()
        }
        return Datum.bool(value.type.length == length)
    }
}
