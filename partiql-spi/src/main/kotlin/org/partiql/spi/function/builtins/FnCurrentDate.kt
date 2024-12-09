// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.types.PType

internal val Fn_CURRENT_DATE____DATE = Function.static(

    name = "current_date",
    returns = PType.date(),
    parameters = arrayOf(),

) { args ->
    TODO("Function current_date not implemented")
}
