// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils

internal val Fn_IS_INTERVAL__ANY__BOOL = FunctionUtils.hidden(

    name = "is_interval",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    TODO("INTERVAL NOT SUPPORTED YET")
}
