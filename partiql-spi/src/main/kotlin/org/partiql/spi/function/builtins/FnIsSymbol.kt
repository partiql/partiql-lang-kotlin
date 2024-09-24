// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_SYMBOL__ANY__BOOL = Function.standard(

    name = "is_symbol",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

    ) { args ->
    Datum.bool(args[0].type.kind == PType.Kind.SYMBOL)
}
