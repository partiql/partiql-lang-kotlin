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
internal object Fn_DATE_ADD_SECOND__INT32_DATE__DATE : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT64_DATE__DATE : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT_DATE__DATE : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT32_TIME__TIME : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT64_TIME__TIME : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT_TIME__TIME : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_second",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_second not implemented")
    }
}
