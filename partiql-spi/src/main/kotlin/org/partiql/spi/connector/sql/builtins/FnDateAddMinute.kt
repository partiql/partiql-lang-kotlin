// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT32_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT64_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT32_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT64_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT32_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT64_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_MINUTE__INT_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_minute not implemented")
    }
}
