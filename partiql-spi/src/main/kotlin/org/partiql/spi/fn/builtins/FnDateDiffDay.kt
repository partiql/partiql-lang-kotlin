// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_DATE_DIFF_DAY__DATE_DATE__INT64 : Fn {

    override val signature = FnSignature(
        name = "date_diff_day",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("datetime1", PType.date()),
            FnParameter("datetime2", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function date_diff_day not implemented")
    }
}

internal object Fn_DATE_DIFF_DAY__TIMESTAMP_TIMESTAMP__INT64 : Fn {

    override val signature = FnSignature(
        name = "date_diff_day",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("datetime1", PType.timestamp(6)),
            FnParameter("datetime2", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function date_diff_day not implemented")
    }
}
