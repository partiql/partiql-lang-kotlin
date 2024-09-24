// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.StringUtils.codepointTrimLeading
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_TRIM_LEADING__STRING__STRING : Function {

    override val signature = FnSignature(
        name = "trim_leading",
        returns = PType.string(),
        parameters = listOf(Parameter("value", PType.string())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val result = value.codepointTrimLeading()
        return Datum.string(result)
    }
}

internal object Fn_TRIM_LEADING__SYMBOL__SYMBOL : Function {

    override val signature = FnSignature(
        name = "trim_leading",
        returns = PType.symbol(),
        parameters = listOf(Parameter("value", PType.symbol())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val result = value.codepointTrimLeading()
        return Datum.symbol(result)
    }
}

internal object Fn_TRIM_LEADING__CLOB__CLOB : Function {

    override val signature = FnSignature(
        name = "trim_leading",
        returns = PType.clob(Int.MAX_VALUE),
        parameters = listOf(Parameter("value", PType.clob(Int.MAX_VALUE))),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].bytes.toString(Charsets.UTF_8)
        val result = string.codepointTrimLeading()
        return Datum.clob(result.toByteArray())
    }
}
