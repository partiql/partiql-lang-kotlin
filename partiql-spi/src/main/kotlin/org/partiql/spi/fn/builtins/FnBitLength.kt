package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

// SQL spec section 6.17 contains <bit length expression>
internal object Fn_BIT_LENGTH__STRING__INT32 : Fn {

    override val signature = FnSignature(
        name = "bit_length",
        returns = PType.integer(),
        parameters = listOf(
            FnParameter("value", PType.string()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val length = value.toByteArray(Charsets.UTF_8).size
        return Datum.integer(length * 8)
    }
}

internal object Fn_BIT_LENGTH__CLOB__INT32 : Fn {

    override val signature = FnSignature(
        name = "bit_length",
        returns = PType.integer(),
        parameters = listOf(
            FnParameter("lhs", PType.clob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bytes
        return Datum.integer(value.size * 8)
    }
}
