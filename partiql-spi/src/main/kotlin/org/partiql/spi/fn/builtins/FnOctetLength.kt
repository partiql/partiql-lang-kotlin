package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_OCTET_LENGTH__STRING__INT32 : Function {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("value", PType.string()),
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

internal object Fn_OCTET_LENGTH__SYMBOL__INT32 : Function {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("value", PType.symbol()),
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

internal object Fn_OCTET_LENGTH__CLOB__INT32 : Function {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("value", PType.clob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bytes
        return Datum.integer(value.size)
    }
}
