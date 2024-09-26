// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_LOWER__STRING__STRING = Function.static(

    name = "lower",
    returns = PType.string(),
    parameters = arrayOf(Parameter("value", PType.string())),

) { args ->
    val string = args[0].string
    val result = string.lowercase()
    Datum.string(result)
}

internal val Fn_LOWER__SYMBOL__SYMBOL = Function.static(

    name = "lower",
    returns = PType.symbol(),
    parameters = arrayOf(Parameter("value", PType.symbol())),

) { args ->
    val string = args[0].string
    val result = string.lowercase()
    Datum.string(result)
}

internal val Fn_LOWER__CLOB__CLOB = Function.static(

    name = "lower",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(Parameter("value", PType.clob(Int.MAX_VALUE))),

) { args ->
    val string = args[0].string
    val result = string.lowercase()
    Datum.string(result)
}
