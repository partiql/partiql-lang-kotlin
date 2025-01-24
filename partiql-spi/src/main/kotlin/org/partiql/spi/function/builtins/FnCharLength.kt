// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_CHAR_LENGTH__STRING__INT = Function.overload(

    name = "char_length",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
    ),

) { args ->
    val value = args[0].string
    Datum.integer(value.codePointCount(0, value.length))
}

internal val Fn_CHAR_LENGTH__CLOB__INT = Function.overload(

    name = "char_length",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("lhs", PType.clob(Int.MAX_VALUE)),
    ),

) { args ->
    val value = args[0].bytes
    Datum.integer(value.size)
}
