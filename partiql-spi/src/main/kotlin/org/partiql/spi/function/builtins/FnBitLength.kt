package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

// SQL spec section 6.17 contains <bit length expression>
internal val Fn_BIT_LENGTH__STRING__INT32 = Function.overload(

    name = "bit_length",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
    ),

) { args ->
    val value = args[0].string
    val length = value.toByteArray(Charsets.UTF_8).size
    Datum.integer(length * 8)
}

internal val Fn_BIT_LENGTH__CLOB__INT32 = Function.overload(

    name = "bit_length",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("lhs", PType.clob(Long.MAX_VALUE)),
    ),

) { args ->
    val value = args[0].bytes
    Datum.integer(value.size * 8)
}
