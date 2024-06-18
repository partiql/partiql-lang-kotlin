// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.BIGINT
import org.partiql.value.PType.Kind.TIME
import org.partiql.value.PType.Kind.TIMESTAMP


internal object Fn_DATE_DIFF_MINUTE__TIME_TIME__BIGINT : Routine {

    override val signature = FnSignature(
        name = "date_diff_minute",
        returns = BIGINT,
        parameters = listOf(
            FnParameter("datetime1", TIME),
            FnParameter("datetime2", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function date_diff_minute not implemented")
    }
}


internal object Fn_DATE_DIFF_MINUTE__TIMESTAMP_TIMESTAMP__BIGINT : Routine {

    override val signature = FnSignature(
        name = "date_diff_minute",
        returns = BIGINT,
        parameters = listOf(
            FnParameter("datetime1", TIMESTAMP),
            FnParameter("datetime2", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function date_diff_minute not implemented")
    }
}
