// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__STRING_INT64__STRING : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = STRING,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("start", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__STRING_INT64_INT64__STRING : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = STRING,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("start", INT64),
            FnParameter("end", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__SYMBOL_INT64__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = SYMBOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("start", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__SYMBOL_INT64_INT64__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = SYMBOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("start", INT64),
            FnParameter("end", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__CLOB_INT64__CLOB : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = CLOB,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("start", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__CLOB_INT64_INT64__CLOB : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = CLOB,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("start", INT64),
            FnParameter("end", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}
