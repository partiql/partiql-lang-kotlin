// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_DATE_DIFF_SECOND__TIME_TIME__INT64 : Function {

    override val signature = FnSignature(
        name = "date_diff_second",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("datetime1", PType.time(6)),
            Parameter("datetime2", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function date_diff_second not implemented")
    }
}

internal object Fn_DATE_DIFF_SECOND__TIMESTAMP_TIMESTAMP__INT64 : Function {

    override val signature = FnSignature(
        name = "date_diff_second",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("datetime1", PType.timestamp(6)),
            Parameter("datetime2", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function date_diff_second not implemented")
    }
}
