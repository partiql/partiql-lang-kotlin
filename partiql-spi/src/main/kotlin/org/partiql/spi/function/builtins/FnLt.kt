// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_LT__INT8_INT8__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.tinyint()),
        Parameter("rhs", PType.tinyint()),
    ),

) { args ->
    val lhs = args[0].byte
    val rhs = args[1].byte
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__INT16_INT16__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.smallint()),
        Parameter("rhs", PType.smallint()),
    ),

) { args ->
    val lhs = args[0].short
    val rhs = args[1].short
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__INT32_INT32__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.integer()),
        Parameter("rhs", PType.integer()),
    ),

) { args ->
    val lhs = args[0].int
    val rhs = args[1].int
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__INT64_INT64__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.bigint()),
        Parameter("rhs", PType.bigint()),
    ),

) { args ->
    val lhs = args[0].long
    val rhs = args[1].long
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__INT_INT__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("lhs", PType.numeric()),
        @Suppress("DEPRECATION") Parameter("rhs", PType.numeric()),
    ),

) { args ->
    val lhs = args[0].bigInteger
    val rhs = args[1].bigInteger
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("lhs", PType.decimal()),
        @Suppress("DEPRECATION") Parameter("rhs", PType.decimal()),
    ),

) { args ->
    val lhs = args[0].bigDecimal
    val rhs = args[1].bigDecimal
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__FLOAT32_FLOAT32__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.real()),
        Parameter("rhs", PType.real()),
    ),

) { args ->
    val lhs = args[0].float
    val rhs = args[1].float
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__FLOAT64_FLOAT64__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.doublePrecision()),
        Parameter("rhs", PType.doublePrecision()),
    ),

) { args ->
    val lhs = args[0].double
    val rhs = args[1].double
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__STRING_STRING__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.string()),
        Parameter("rhs", PType.string()),
    ),

) { args ->
    val lhs = args[0].string
    val rhs = args[1].string
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__SYMBOL_SYMBOL__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.symbol()),
        Parameter("rhs", PType.symbol()),
    ),

) { args ->
    val lhs = args[0].string
    val rhs = args[1].string
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__DATE_DATE__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.date()),
        Parameter("rhs", PType.date()),
    ),

) { args ->
    val lhs = args[0].date
    val rhs = args[1].date
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__TIME_TIME__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.time(6)),
        Parameter("rhs", PType.time(6)),
    ),

) { args ->
    val lhs = args[0].time
    val rhs = args[1].time
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__TIMESTAMP_TIMESTAMP__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.timestamp(6)),
        Parameter("rhs", PType.timestamp(6)),
    ),

) { args ->
    val lhs = args[0].timestamp
    val rhs = args[1].timestamp
    Datum.bool(lhs < rhs)
}

internal val Fn_LT__BOOL_BOOL__BOOL = Function.static(

    name = "lt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.bool()),
        Parameter("rhs", PType.bool()),
    ),

) { args ->
    val lhs = args[0].boolean
    val rhs = args[1].boolean
    Datum.bool(lhs < rhs)
}
