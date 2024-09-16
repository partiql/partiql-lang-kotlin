// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.types.PType

internal object Fn_DATE_DIFF_MINUTE__TIME_TIME__INT64 : Function {

    override val signature = FnSignature(
        name = "date_diff_minute",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("datetime1", PType.time(6)),
            Parameter("datetime2", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function date_diff_minute not implemented")
    }
}

internal object Fn_DATE_DIFF_MINUTE__TIMESTAMP_TIMESTAMP__INT64 : Function {

    override val signature = FnSignature(
        name = "date_diff_minute",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("datetime1", PType.timestamp(6)),
            Parameter("datetime2", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function date_diff_minute not implemented")
    }
}
