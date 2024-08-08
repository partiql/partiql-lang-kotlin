package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_OCTET_LENGTH__STRING__INT32 : Fn {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("value", PType.typeString()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val length = value.toByteArray(Charsets.UTF_8).size
        return Datum.integer(length)
    }
}

internal object Fn_OCTET_LENGTH__SYMBOL__INT32 : Fn {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("value", PType.typeSymbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val length = value.toByteArray(Charsets.UTF_8).size
        return Datum.integer(length)
    }
}

internal object Fn_OCTET_LENGTH__CLOB__INT32 : Fn {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("value", PType.typeClob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bytes
        return Datum.integer(value.size)
    }
}
