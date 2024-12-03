// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.types.PType

internal val Fn_CURRENT_USER____STRING = Function.static(

    name = "current_user",
    returns = PType.string(),
    parameters = arrayOf(),

) { args ->
    TODO("Function current_user not implemented")
}
