// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal val Fn_IS_BYTE__ANY__BOOL = Function.static(

    name = "is_byte",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    TODO("BYTE NOT SUPPORTED YET.")
}
