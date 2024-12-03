// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_UPPER__STRING__STRING = Function.static(

    name = "upper",
    returns = PType.string(),
    parameters = arrayOf(Parameter("value", PType.string())),

) { args ->
    val string = args[0].string
    val result = string.uppercase()
    Datum.string(result)
}

internal val Fn_UPPER__CLOB__CLOB = Function.static(

    name = "upper",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(Parameter("value", PType.clob(Int.MAX_VALUE))),

) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val result = string.uppercase()
    Datum.clob(result.toByteArray())
}
