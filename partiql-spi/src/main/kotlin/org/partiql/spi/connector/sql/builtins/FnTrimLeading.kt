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
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_LEADING__STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "trim_leading",
        returns = STRING,
        parameters = listOf(FnParameter("value", STRING),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_leading not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_LEADING__SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "trim_leading",
        returns = SYMBOL,
        parameters = listOf(FnParameter("value", SYMBOL),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_leading not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_LEADING__CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "trim_leading",
        returns = CLOB,
        parameters = listOf(FnParameter("value", CLOB),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_leading not implemented")
    }
}
