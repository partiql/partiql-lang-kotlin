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
internal object Fn_TRIM_TRAILING_CHARS__STRING_STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "trim_trailing_chars",
        returns = STRING,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("chars", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_trailing_chars not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_TRAILING_CHARS__SYMBOL_SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "trim_trailing_chars",
        returns = SYMBOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("chars", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_trailing_chars not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_TRAILING_CHARS__CLOB_CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "trim_trailing_chars",
        returns = CLOB,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("chars", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_trailing_chars not implemented")
    }
}
