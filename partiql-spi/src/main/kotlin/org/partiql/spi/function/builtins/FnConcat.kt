// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_CONCAT__STRING_STRING__STRING = Function.static(

    name = "concat",
    returns = PType.string(),
    parameters = arrayOf(
        Parameter("lhs", PType.string()),
        Parameter("rhs", PType.string()),
    ),

) { args ->
    val arg0 = args[0].string
    val arg1 = args[1].string
    Datum.string(arg0 + arg1)
}

internal val Fn_CONCAT__SYMBOL_SYMBOL__SYMBOL = Function.static(

    name = "concat",
    returns = PType.symbol(),
    parameters = arrayOf(
        Parameter("lhs", PType.symbol()),
        Parameter("rhs", PType.symbol()),
    ),

) { args ->
    val arg0 = args[0].string
    val arg1 = args[1].string
    Datum.symbol(arg0 + arg1)
}

internal val Fn_CONCAT__CLOB_CLOB__CLOB = Function.static(

    name = "concat",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(
        Parameter("lhs", PType.clob(Int.MAX_VALUE)),
        Parameter("rhs", PType.clob(Int.MAX_VALUE)),
    ),

) { args ->
    val arg0 = args[0].bytes
    val arg1 = args[1].bytes
    Datum.clob(arg0 + arg1)
}
