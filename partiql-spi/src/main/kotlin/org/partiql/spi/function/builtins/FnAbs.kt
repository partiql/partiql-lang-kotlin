// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.math.absoluteValue

// TODO: When negate a negative value, we need to consider overflow
internal val Fn_ABS__INT8__INT8 = Function.static(
    name = "abs",
    parameters = arrayOf(Parameter("value", PType.tinyint())),
    returns = PType.tinyint(),
) { args ->
    @Suppress("DEPRECATION")
    val value = args[0].byte
    if (value < 0) Datum.tinyint(value.times(-1).toByte()) else Datum.tinyint(value)
}

internal val Fn_ABS__INT16__INT16 = Function.static(
    name = "abs",
    returns = PType.smallint(),
    parameters = arrayOf(Parameter("value", PType.smallint())),
) { args ->
    val value = args[0].short
    if (value < 0) Datum.smallint(value.times(-1).toShort()) else Datum.smallint(value)
}

internal val Fn_ABS__INT32__INT32 = Function.static(
    name = "abs",
    returns = PType.integer(),
    parameters = arrayOf(Parameter("value", PType.integer())),
) { args ->
    val value = args[0].int
    Datum.integer(value.absoluteValue)
}

internal val Fn_ABS__INT64__INT64 = Function.static(
    name = "abs",
    returns = PType.bigint(),
    parameters = arrayOf(Parameter("value", PType.bigint())),
) { args ->
    val value = args[0].long
    Datum.bigint(value.absoluteValue)
}

internal val Fn_ABS__NUMERIC__NUMERIC = Function.static(
    name = "abs",
    returns = DefaultNumeric.NUMERIC,
    parameters = arrayOf(Parameter("value", DefaultNumeric.NUMERIC)),
) { args ->
    val value = args[0].bigDecimal
    Datum.numeric(value.abs())
}

internal val Fn_ABS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Function.static(
    name = "abs",
    returns = PType.decimal(39, 19), // TODO: Rewrite using new function modeling.
    parameters = arrayOf(Parameter("value", PType.decimal(38, 19))),
) { args ->
    val value = args[0].bigDecimal
    Datum.decimal(value.abs())
}

internal val Fn_ABS__FLOAT32__FLOAT32 = Function.static(
    name = "abs",
    returns = PType.real(),
    parameters = arrayOf(Parameter("value", PType.real())),
) { args ->
    val value = args[0].float
    Datum.real(value.absoluteValue)
}

internal val Fn_ABS__FLOAT64__FLOAT64 = Function.static(
    name = "abs",
    returns = PType.doublePrecision(),
    parameters = arrayOf(Parameter("value", PType.doublePrecision())),
) { args ->
    val value = args[0].double
    Datum.doublePrecision(value.absoluteValue)
}
