// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.*

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_DIFF_MINUTE__DATE_DATE__INT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_diff_minute",
        returns = INT64,
        parameters = listOf(
            FnParameter("datetime1", DATE),
            FnParameter("datetime2", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_DIFF_MINUTE__TIME_TIME__INT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_diff_minute",
        returns = INT64,
        parameters = listOf(
            FnParameter("datetime1", TIME),
            FnParameter("datetime2", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_DIFF_MINUTE__TIMESTAMP_TIMESTAMP__INT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_diff_minute",
        returns = INT64,
        parameters = listOf(
            FnParameter("datetime1", TIMESTAMP),
            FnParameter("datetime2", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_minute not implemented")
    }
}
