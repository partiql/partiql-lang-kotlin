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
internal object Fn_POSITION__STRING_STRING__INT64 : Fn {

    override val signature = FnSignature(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FnParameter("probe", STRING),
            FnParameter("value", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function position not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POSITION__SYMBOL_SYMBOL__INT64 : Fn {

    override val signature = FnSignature(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FnParameter("probe", SYMBOL),
            FnParameter("value", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function position not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POSITION__CLOB_CLOB__INT64 : Fn {

    override val signature = FnSignature(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FnParameter("probe", CLOB),
            FnParameter("value", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function position not implemented")
    }
}
