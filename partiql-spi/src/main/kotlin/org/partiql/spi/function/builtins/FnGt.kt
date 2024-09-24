// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_GT__INT8_INT8__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.tinyint()),
        Parameter("rhs", PType.tinyint()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.byte > rhs.byte)
}

internal val Fn_GT__INT16_INT16__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.smallint()),
        Parameter("rhs", PType.smallint()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.short > rhs.short)
}

internal val Fn_GT__INT32_INT32__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.integer()),
        Parameter("rhs", PType.integer()),
    ),

    ) { args ->
    val lhs = args[0].int
    val rhs = args[1].int
    Datum.bool(lhs > rhs)
}

internal val Fn_GT__INT64_INT64__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.bigint()),
        Parameter("rhs", PType.bigint()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.long > rhs.long)
}

internal val Fn_GT__INT_INT__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("lhs", PType.numeric()),
        @Suppress("DEPRECATION") Parameter("rhs", PType.numeric()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.bigInteger > rhs.bigInteger)
}

internal val Fn_GT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("lhs", PType.decimal()),
        @Suppress("DEPRECATION") Parameter("rhs", PType.decimal()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.bigDecimal > rhs.bigDecimal)
}

internal val Fn_GT__FLOAT32_FLOAT32__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.real()),
        Parameter("rhs", PType.real()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.float > rhs.float)
}

internal val Fn_GT__FLOAT64_FLOAT64__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.doublePrecision()),
        Parameter("rhs", PType.doublePrecision()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.double > rhs.double)
}

internal val Fn_GT__STRING_STRING__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.string()),
        Parameter("rhs", PType.string()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.string > rhs.string)
}

internal val Fn_GT__SYMBOL_SYMBOL__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.symbol()),
        Parameter("rhs", PType.symbol()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.string > rhs.string)
}

internal val Fn_GT__DATE_DATE__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.date()),
        Parameter("rhs", PType.date()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.date > rhs.date)
}

internal val Fn_GT__TIME_TIME__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.time(6)),
        Parameter("rhs", PType.time(6)),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.time > rhs.time)
}

internal val Fn_GT__TIMESTAMP_TIMESTAMP__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.timestamp(6)),
        Parameter("rhs", PType.timestamp(6)),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.timestamp > rhs.timestamp)
}

internal val Fn_GT__BOOL_BOOL__BOOL = Function.standard(

    name = "gt",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.bool()),
        Parameter("rhs", PType.bool()),
    ),

    ) { args ->
    val lhs = args[0]
    val rhs = args[1]
    Datum.bool(lhs.boolean > rhs.boolean)
}
