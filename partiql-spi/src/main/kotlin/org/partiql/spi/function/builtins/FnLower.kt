// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_LOWER__CHAR__CHAR = Function.overload(

    name = "lower",
    returns = PType.character(),
    parameters = arrayOf(Parameter("value", PType.character())),

) { args ->
    TODO("Not yet implemented")
}

internal val Fn_LOWER__VARCHAR__VARCHAR = Function.overload(

    name = "lower",
    returns = PType.varchar(),
    parameters = arrayOf(Parameter("value", PType.varchar())),

) { args ->
    TODO("Not yet implemented")
}

internal val Fn_LOWER__STRING__STRING = Function.overload(

    name = "lower",
    returns = PType.string(),
    parameters = arrayOf(Parameter("value", PType.string())),

) { args ->
    val string = args[0].string
    val result = string.lowercase()
    Datum.string(result)
}

internal val Fn_LOWER__CLOB__CLOB = Function.overload(

    name = "lower",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(Parameter("value", PType.clob(Int.MAX_VALUE))),

) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val result = string.lowercase()
    Datum.clob(result.toByteArray())
}
