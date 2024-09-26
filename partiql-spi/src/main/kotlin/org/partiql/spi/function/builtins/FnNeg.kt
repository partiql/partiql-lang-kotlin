// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

// TODO: Handle Overflow
internal val Fn_NEG__INT8__INT8 = Function.static(

    name = "neg",
    returns = PType.tinyint(),
    parameters = arrayOf(Parameter("value", PType.tinyint())),

) { args ->
    @Suppress("DEPRECATION")
    val value = args[0].byte
    Datum.tinyint(value.times(-1).toByte())
}

internal val Fn_NEG__INT16__INT16 = Function.static(

    name = "neg",
    returns = PType.smallint(),
    parameters = arrayOf(Parameter("value", PType.smallint())),

) { args ->
    val value = args[0].short
    Datum.smallint(value.times(-1).toShort())
}

internal val Fn_NEG__INT32__INT32 = Function.static(

    name = "neg",
    returns = PType.integer(),
    parameters = arrayOf(Parameter("value", PType.integer())),

) { args ->
    val value = args[0].int
    Datum.integer(value.times(-1))
}

internal val Fn_NEG__INT64__INT64 = Function.static(

    name = "neg",
    returns = PType.bigint(),
    parameters = arrayOf(Parameter("value", PType.bigint())),

) { args ->
    val value = args[0].long
    Datum.bigint(value.times(-1L))
}

internal val Fn_NEG__INT__INT = Function.static(

    name = "neg",
    returns = PType.numeric(),
    parameters = arrayOf(Parameter("value", PType.numeric())),

) { args ->
    val value = args[0].bigInteger
    Datum.numeric(value.negate())
}

internal val Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Function.static(

    name = "neg",
    returns = PType.decimal(),
    parameters = arrayOf(Parameter("value", PType.decimal())),

) { args ->
    val value = args[0].bigDecimal
    Datum.decimal(value.negate())
}

internal val Fn_NEG__FLOAT32__FLOAT32 = Function.static(

    name = "neg",
    returns = PType.real(),
    parameters = arrayOf(Parameter("value", PType.real())),

) { args ->
    val value = args[0].float
    Datum.real(value.times(-1))
}

internal val Fn_NEG__FLOAT64__FLOAT64 = Function.static(

    name = "neg",
    returns = PType.doublePrecision(),
    parameters = arrayOf(Parameter("value", PType.doublePrecision())),

) { args ->
    val value = args[0].double
    Datum.doublePrecision(value.times(-1))
}
