// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_CONCAT__CHAR_CHAR__CHAR = Function.static(
    name = "concat",
    returns = PType.character(256), // TODO: Handle length
    parameters = arrayOf(
        Parameter("lhs", PType.character(256)), // TODO: Handle length
        Parameter("rhs", PType.character(256)), // TODO: Handle length
    ),
) { args ->
    val arg0 = args[0].string
    val arg1 = args[1].string
    Datum.character(arg0 + arg1, 256)
}

internal val Fn_CONCAT__VARCHAR_VARCHAR__VARCHAR = Function.static(
    name = "concat",
    returns = PType.varchar(256), // TODO: Handle length
    parameters = arrayOf(
        Parameter("lhs", PType.varchar(256)), // TODO: Handle length
        Parameter("rhs", PType.varchar(256)), // TODO: Handle length
    ),
) { args ->
    val arg0 = args[0].string
    val arg1 = args[1].string
    Datum.varchar(arg0 + arg1, 256)
}

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
