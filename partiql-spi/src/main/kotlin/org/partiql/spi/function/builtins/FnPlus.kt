// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

// TODO: Handle Overflow
internal val Fn_PLUS__INT8_INT8__INT8 = Function.static(

    name = "plus",
    returns = PType.tinyint(),
    parameters = arrayOf(
        Parameter("lhs", PType.tinyint()),
        Parameter("rhs", PType.tinyint()),
    ),

) { args ->
    @Suppress("DEPRECATION") val arg0 = args[0].byte
    @Suppress("DEPRECATION") val arg1 = args[1].byte
    Datum.tinyint((arg0 + arg1).toByte())
}

internal val Fn_PLUS__INT16_INT16__INT16 = Function.static(

    name = "plus",
    returns = PType.smallint(),
    parameters = arrayOf(
        Parameter("lhs", PType.smallint()),
        Parameter("rhs", PType.smallint()),
    ),

) { args ->
    val arg0 = args[0].short
    val arg1 = args[1].short
    Datum.smallint((arg0 + arg1).toShort())
}

internal val Fn_PLUS__INT32_INT32__INT32 = Function.static(

    name = "plus",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("lhs", PType.integer()),
        Parameter("rhs", PType.integer()),
    ),

) { args ->
    val arg0 = args[0].int
    val arg1 = args[1].int
    Datum.integer(arg0 + arg1)
}

internal val Fn_PLUS__INT64_INT64__INT64 = Function.static(

    name = "plus",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("lhs", PType.bigint()),
        Parameter("rhs", PType.bigint()),
    ),

) { args ->
    val arg0 = args[0].long
    val arg1 = args[1].long
    Datum.bigint(arg0 + arg1)
}

internal val Fn_PLUS__INT_INT__INT = Function.static(

    name = "plus",
    returns = PType.numeric(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("lhs", PType.numeric()),
        @Suppress("DEPRECATION") Parameter("rhs", PType.numeric()),
    ),

) { args ->
    val arg0 = args[0].bigInteger
    val arg1 = args[1].bigInteger
    Datum.numeric(arg0 + arg1)
}

internal val Fn_PLUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Function.static(

    name = "plus",
    returns = PType.decimal(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("lhs", PType.decimal()),
        @Suppress("DEPRECATION") Parameter("rhs", PType.decimal()),
    ),

) { args ->
    val arg0 = args[0].bigDecimal
    val arg1 = args[1].bigDecimal
    Datum.decimal(arg0 + arg1)
}

internal val Fn_PLUS__FLOAT32_FLOAT32__FLOAT32 = Function.static(

    name = "plus",
    returns = PType.real(),
    parameters = arrayOf(
        Parameter("lhs", PType.real()),
        Parameter("rhs", PType.real()),
    ),

) { args ->
    val arg0 = args[0].float
    val arg1 = args[1].float
    Datum.real(arg0 + arg1)
}

internal val Fn_PLUS__FLOAT64_FLOAT64__FLOAT64 = Function.static(

    name = "plus",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("lhs", PType.doublePrecision()),
        Parameter("rhs", PType.doublePrecision()),
    ),

) { args ->
    val arg0 = args[0].double
    val arg1 = args[1].double
    Datum.doublePrecision(arg0 + arg1)
}
