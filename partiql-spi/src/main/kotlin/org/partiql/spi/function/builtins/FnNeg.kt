// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

internal val Fn_NEG__INT8__INT8 = FunctionUtils.hidden(

    name = "neg",
    returns = PType.tinyint(),
    parameters = arrayOf(Parameter("value", PType.tinyint())),

) { args ->
    val value = args[0].byte
    if (value == Byte.MIN_VALUE) {
        throw PErrors.numericValueOutOfRangeException("-($value)", PType.tinyint())
    } else {
        Datum.tinyint(value.times(-1).toByte())
    }
}

internal val Fn_NEG__INT16__INT16 = FunctionUtils.hidden(

    name = "neg",
    returns = PType.smallint(),
    parameters = arrayOf(Parameter("value", PType.smallint())),

) { args ->
    val value = args[0].short
    if (value == Short.MIN_VALUE) {
        throw PErrors.numericValueOutOfRangeException("-($value)", PType.smallint())
    } else {
        Datum.smallint(value.times(-1).toShort())
    }
}

internal val Fn_NEG__INT32__INT32 = FunctionUtils.hidden(

    name = "neg",
    returns = PType.integer(),
    parameters = arrayOf(Parameter("value", PType.integer())),

) { args ->
    val value = args[0].int
    if (value == Int.MIN_VALUE) {
        throw PErrors.numericValueOutOfRangeException("-($value)", PType.integer())
    } else {
        Datum.integer(value.times(-1))
    }
}

internal val Fn_NEG__INT64__INT64 = FunctionUtils.hidden(

    name = "neg",
    returns = PType.bigint(),
    parameters = arrayOf(Parameter("value", PType.bigint())),

) { args ->
    val value = args[0].long
    if (value == Long.MIN_VALUE) {
        throw PErrors.numericValueOutOfRangeException("-($value)", PType.bigint())
    } else {
        Datum.bigint(value * -1L)
    }
}

internal val Fn_NEG__INT__INT = FunctionUtils.hidden(

    name = "neg",
    returns = DefaultNumeric.NUMERIC,
    parameters = arrayOf(Parameter("value", DefaultNumeric.NUMERIC)),

) { args ->
    val value = args[0].bigDecimal
    Datum.numeric(value.negate())
}

internal val Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = FunctionUtils.hidden(

    name = "neg",
    returns = PType.decimal(38, 19), // TODO: Rewrite using new function modeling.
    parameters = arrayOf(Parameter("value", PType.decimal(38, 19))),

) { args ->
    val value = args[0].bigDecimal
    Datum.decimal(value.negate())
}

internal val Fn_NEG__FLOAT32__FLOAT32 = FunctionUtils.hidden(

    name = "neg",
    returns = PType.real(),
    parameters = arrayOf(Parameter("value", PType.real())),

) { args ->
    val value = args[0].float
    Datum.real(value.times(-1))
}

internal val Fn_NEG__FLOAT64__FLOAT64 = FunctionUtils.hidden(

    name = "neg",
    returns = PType.doublePrecision(),
    parameters = arrayOf(Parameter("value", PType.doublePrecision())),

) { args ->
    val value = args[0].double
    Datum.doublePrecision(value.times(-1))
}
