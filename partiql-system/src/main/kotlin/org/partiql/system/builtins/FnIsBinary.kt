// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal val Fn_IS_BINARY__ANY__BOOL = Function.static(
    name = "is_binary",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),
) { _ ->
    TODO("BINARY NOT SUPPORTED RIGHT NOW.")
}
