// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_UPPER__CHAR__CHAR = Function.overload(

    name = "upper",
    returns = PType.character(),
    parameters = arrayOf(Parameter("value", PType.character())),

) { args ->
    TODO("Not yet implemented")
}

internal val Fn_UPPER__VARCHAR__VARCHAR = Function.overload(

    name = "upper",
    returns = PType.varchar(),
    parameters = arrayOf(Parameter("value", PType.varchar())),

) { args ->
    TODO("Not yet implemented")
}

internal val Fn_UPPER__STRING__STRING = Function.overload(

    name = "upper",
    returns = PType.string(),
    parameters = arrayOf(Parameter("value", PType.string())),

) { args ->
    val string = args[0].string
    val result = string.uppercase()
    Datum.string(result)
}

internal val Fn_UPPER__CLOB__CLOB = Function.overload(

    name = "upper",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(Parameter("value", PType.clob(Int.MAX_VALUE))),

) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val result = string.uppercase()
    Datum.clob(result.toByteArray())
}
