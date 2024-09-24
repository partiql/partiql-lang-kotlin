// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.StringUtils.codepointPosition
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_POSITION__STRING_STRING__INT64 : Function {

    override val signature = FnSignature(
        name = "position",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("probe", PType.string()),
            Parameter("value", PType.string()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val s1 = args[0].string
        val s2 = args[1].string
        val result = s2.codepointPosition(s1)
        return Datum.bigint(result.toLong())
    }
}

internal object Fn_POSITION__SYMBOL_SYMBOL__INT64 : Function {

    override val signature = FnSignature(
        name = "position",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("probe", PType.symbol()),
            Parameter("value", PType.symbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val s1 = args[0].string
        val s2 = args[1].string
        val result = s2.codepointPosition(s1)
        return Datum.bigint(result.toLong())
    }
}

internal object Fn_POSITION__CLOB_CLOB__INT64 : Function {

    override val signature = FnSignature(
        name = "position",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("probe", PType.clob(Int.MAX_VALUE)),
            Parameter("value", PType.clob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val s1 = args[0].bytes.toString(Charsets.UTF_8)
        val s2 = args[1].bytes.toString(Charsets.UTF_8)
        val result = s2.codepointPosition(s1)
        return Datum.bigint(result.toLong())
    }
}
