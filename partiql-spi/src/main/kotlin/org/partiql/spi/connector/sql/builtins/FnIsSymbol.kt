// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.SymbolValue
import org.partiql.value.boolValue

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_SYMBOL__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_symbol",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY)),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(args[0] is SymbolValue)
    }
}
