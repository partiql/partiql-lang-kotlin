// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType

internal val Fn_CURRENT_USER____STRING = FunctionUtils.hidden(

    name = "current_user",
    returns = PType.string(),
    parameters = arrayOf(),

) { args ->
    TODO("Function current_user not implemented")
}
