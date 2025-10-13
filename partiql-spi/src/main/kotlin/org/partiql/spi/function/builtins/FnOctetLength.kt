package org.partiql.spi.function.builtins

// TODO: add support for CHAR/VARCHAR - https://github.com/partiql/partiql-lang-kotlin/issues/1838
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_OCTET_LENGTH__STRING__INT32 = Function.overload(

    name = "octet_length",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
    ),

) { args ->
    val value = args[0].string
    val length = value.toByteArray(Charsets.UTF_8).size
    Datum.integer(length)
}

internal val Fn_OCTET_LENGTH__CLOB__INT32 = Function.overload(

    name = "octet_length",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("value", PType.clob(Int.MAX_VALUE)),
    ),

) { args ->
    val value = args[0].bytes
    Datum.integer(value.size)
}
