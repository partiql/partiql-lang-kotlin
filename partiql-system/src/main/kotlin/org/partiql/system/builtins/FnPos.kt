// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal val Fn_POS__INT8__INT8 = Function.static(

    name = "pos",
    returns = PType.tinyint(),
    parameters = arrayOf(Parameter("value", PType.tinyint())),

) { args ->
    args[0]
}

internal val Fn_POS__INT16__INT16 = Function.static(

    name = "pos",
    returns = PType.smallint(),
    parameters = arrayOf(Parameter("value", PType.smallint())),

) { args ->
    args[0]
}

internal val Fn_POS__INT32__INT32 = Function.static(

    name = "pos",
    returns = PType.integer(),
    parameters = arrayOf(Parameter("value", PType.integer())),

) { args ->
    args[0]
}

internal val Fn_POS__INT64__INT64 = Function.static(

    name = "pos",
    returns = PType.bigint(),
    parameters = arrayOf(Parameter("value", PType.bigint())),

) { args ->
    args[0]
}

internal val Fn_POS__INT__INT = Function.static(

    name = "pos",
    returns = DefaultNumeric.NUMERIC,
    parameters = arrayOf(Parameter("value", DefaultNumeric.NUMERIC)),

) { args ->
    args[0]
}

internal val Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Function.static(

    name = "pos",
    returns = PType.decimal(38, 19), // TODO: Rewrite this using the new modeling
    parameters = arrayOf(Parameter("value", PType.decimal(38, 19))),

) { args ->
    args[0]
}

internal val Fn_POS__FLOAT32__FLOAT32 = Function.static(

    name = "pos",
    returns = PType.real(),
    parameters = arrayOf(Parameter("value", PType.real())),

) { args ->
    args[0]
}

internal val Fn_POS__FLOAT64__FLOAT64 = Function.static(

    name = "pos",
    returns = PType.doublePrecision(),
    parameters = arrayOf(Parameter("value", PType.doublePrecision())),

) { args ->
    args[0]
}
