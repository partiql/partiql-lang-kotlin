// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorAvg
import org.partiql.spi.types.PType

// TODO: This needs to be formalized. See https://github.com/partiql/partiql-lang-kotlin/issues/1659
private val AVG_DECIMAL = PType.decimal(38, 19)

internal val Agg_AVG__INT8__INT8 = Aggregation.static(
    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(Parameter("value", PType.tinyint())),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__INT16__INT16 = Aggregation.static(
    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(Parameter("value", PType.smallint())),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__INT32__INT32 = Aggregation.static(

    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(
        Parameter("value", PType.integer()),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__INT64__INT64 = Aggregation.static(

    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(
        Parameter("value", PType.bigint()),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__NUMERIC__NUMERIC = Aggregation.static(

    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(
        Parameter("value", DefaultNumeric.NUMERIC),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Aggregation.static(

    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(
        Parameter("value", AVG_DECIMAL),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__FLOAT32__FLOAT32 = Aggregation.static(

    name = "avg",
    returns = PType.real(),
    parameters = arrayOf(
        Parameter("value", PType.real()),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__FLOAT64__FLOAT64 = Aggregation.static(

    name = "avg",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("value", PType.doublePrecision()),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__ANY__ANY = Aggregation.static(

    name = "avg",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorAvg,
)
