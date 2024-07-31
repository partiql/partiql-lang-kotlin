// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.TIMESTAMP

@OptIn(PartiQLValueExperimental::class)
internal object Fn_DATE_DIFF_DAY__DATE_DATE__INT64 : Fn {

    override val signature = FnSignature(
        name = "date_diff_day",
        returns = INT64,
        parameters = listOf(
            FnParameter("datetime1", DATE),
            FnParameter("datetime2", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_DATE_DIFF_DAY__TIMESTAMP_TIMESTAMP__INT64 : Fn {

    override val signature = FnSignature(
        name = "date_diff_day",
        returns = INT64,
        parameters = listOf(
            FnParameter("datetime1", TIMESTAMP),
            FnParameter("datetime2", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_day not implemented")
    }
}
