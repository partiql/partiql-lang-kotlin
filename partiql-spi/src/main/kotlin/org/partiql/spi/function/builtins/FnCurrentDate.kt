// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils

internal val Fn_CURRENT_DATE____DATE = FunctionUtils.hidden(

    name = "current_date",
    returns = PType.date(),
    parameters = arrayOf(),

) { args ->
    TODO("Function current_date not implemented")
}
