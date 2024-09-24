// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_BETWEEN__INT8_INT8_INT8__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.tinyint()),
        Parameter("lower", PType.tinyint()),
        Parameter("upper", PType.tinyint()),
    ),

    ) { args ->
    @Suppress("DEPRECATION") val value = args[0].byte
    @Suppress("DEPRECATION") val lower = args[1].byte
    @Suppress("DEPRECATION") val upper = args[2].byte
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__INT16_INT16_INT16__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.smallint()),
        Parameter("lower", PType.smallint()),
        Parameter("upper", PType.smallint()),
    ),

    ) { args ->
    val value = args[0].short
    val lower = args[1].short
    val upper = args[2].short
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__INT32_INT32_INT32__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.integer()),
        Parameter("lower", PType.integer()),
        Parameter("upper", PType.integer()),
    ),

    ) { args ->
    val value = args[0].int
    val lower = args[1].int
    val upper = args[2].int
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__INT64_INT64_INT64__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.bigint()),
        Parameter("lower", PType.bigint()),
        Parameter("upper", PType.bigint()),
    ),

    ) { args ->
    val value = args[0].long
    val lower = args[1].long
    val upper = args[2].long
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__INT_INT_INT__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
        @Suppress("DEPRECATION") Parameter("lower", PType.numeric()),
        @Suppress("DEPRECATION") Parameter("upper", PType.numeric()),
    ),

    ) { args ->
    val value = args[0].bigInteger
    val lower = args[1].bigInteger
    val upper = args[2].bigInteger
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
        @Suppress("DEPRECATION") Parameter("lower", PType.decimal()),
        @Suppress("DEPRECATION") Parameter("upper", PType.decimal()),
    ),

    ) { args ->
    val value = args[0].bigDecimal
    val lower = args[1].bigDecimal
    val upper = args[2].bigDecimal
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.real()),
        Parameter("lower", PType.real()),
        Parameter("upper", PType.real()),
    ),

    ) { args ->
    val value = args[0].float
    val lower = args[1].float
    val upper = args[2].float
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.doublePrecision()),
        Parameter("lower", PType.doublePrecision()),
        Parameter("upper", PType.doublePrecision()),
    ),

    ) { args ->
    val value = args[0].double
    val lower = args[1].double
    val upper = args[2].double
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__STRING_STRING_STRING__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
        Parameter("lower", PType.string()),
        Parameter("upper", PType.string()),
    ),

    ) { args ->
    val value = args[0].string
    val lower = args[1].string
    val upper = args[2].string
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__SYMBOL_SYMBOL_SYMBOL__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.symbol()),
        Parameter("lower", PType.symbol()),
        Parameter("upper", PType.symbol()),
    ),

    ) { args ->
    val value = args[0].string
    val lower = args[1].string
    val upper = args[2].string
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.clob(Int.MAX_VALUE)),
        Parameter("lower", PType.clob(Int.MAX_VALUE)),
        Parameter("upper", PType.clob(Int.MAX_VALUE)),
    ),

    ) { args ->
    val value = args[0].bytes.toString(Charsets.UTF_8)
    val lower = args[1].bytes.toString(Charsets.UTF_8)
    val upper = args[2].bytes.toString(Charsets.UTF_8)
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__DATE_DATE_DATE__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.date()),
        Parameter("lower", PType.date()),
        Parameter("upper", PType.date()),
    ),

    ) { args ->
    val value = args[0].date
    val lower = args[1].date
    val upper = args[2].date
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__TIME_TIME_TIME__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.time(6)),
        Parameter("lower", PType.time(6)),
        Parameter("upper", PType.time(6)),
    ),

    ) { args ->
    val value = args[0].time
    val lower = args[1].time
    val upper = args[2].time
    Datum.bool(value in lower..upper)
}

internal val Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL = Function.standard(

    name = "between",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.timestamp(6)),
        Parameter("lower", PType.timestamp(6)),
        Parameter("upper", PType.timestamp(6)),
    ),

    ) { args ->
    val value = args[0].timestamp
    val lower = args[1].timestamp
    val upper = args[2].timestamp
    Datum.bool(value in lower..upper)
}
