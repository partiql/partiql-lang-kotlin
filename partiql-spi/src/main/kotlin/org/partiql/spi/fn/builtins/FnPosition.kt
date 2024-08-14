// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.utils.StringUtils.codepointPosition
import org.partiql.types.PType

internal object Fn_POSITION__STRING_STRING__INT64 : Fn {

    override val signature = FnSignature(
        name = "position",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("probe", PType.string()),
            FnParameter("value", PType.string()),
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

internal object Fn_POSITION__CLOB_CLOB__INT64 : Fn {

    override val signature = FnSignature(
        name = "position",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("probe", PType.clob(Int.MAX_VALUE)),
            FnParameter("value", PType.clob(Int.MAX_VALUE)),
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
