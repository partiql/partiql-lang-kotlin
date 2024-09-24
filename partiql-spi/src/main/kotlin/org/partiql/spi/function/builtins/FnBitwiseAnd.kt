// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import kotlin.experimental.and

internal val Fn_BITWISE_AND__INT8_INT8__INT8 = Function.standard(

    name = "bitwise_and",
    returns = PType.tinyint(),
    parameters = arrayOf(
        Parameter("lhs", PType.tinyint()),
        Parameter("rhs", PType.tinyint()),
    ),

    ) { args ->
    @Suppress("DEPRECATION") val arg0 = args[0].byte
    @Suppress("DEPRECATION") val arg1 = args[1].byte
    Datum.tinyint(arg0 and arg1)
}

internal val Fn_BITWISE_AND__INT16_INT16__INT16 = Function.standard(

    name = "bitwise_and",
    returns = PType.smallint(),
    parameters = arrayOf(
        Parameter("lhs", PType.smallint()),
        Parameter("rhs", PType.smallint()),
    ),

    ) { args ->
    val arg0 = args[0].short
    val arg1 = args[1].short
    Datum.smallint(arg0 and arg1)
}

internal val Fn_BITWISE_AND__INT32_INT32__INT32 = Function.standard(

    name = "bitwise_and",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("lhs", PType.integer()),
        Parameter("rhs", PType.integer()),
    ),

    ) { args ->
    val arg0 = args[0].int
    val arg1 = args[1].int
    Datum.integer(arg0 and arg1)
}

internal val Fn_BITWISE_AND__INT64_INT64__INT64 = Function.standard(

    name = "bitwise_and",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("lhs", PType.bigint()),
        Parameter("rhs", PType.bigint()),
    ),

    ) { args ->
    val arg0 = args[0].long
    val arg1 = args[1].long
    Datum.bigint(arg0 and arg1)
}

internal val Fn_BITWISE_AND__INT_INT__INT = Function.standard(

    name = "bitwise_and",
    returns = PType.numeric(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("lhs", PType.numeric()),
        @Suppress("DEPRECATION") Parameter("rhs", PType.numeric()),
    ),

    ) { args ->
    val arg0 = args[0].bigInteger
    val arg1 = args[1].bigInteger
    Datum.numeric(arg0 and arg1)
}
