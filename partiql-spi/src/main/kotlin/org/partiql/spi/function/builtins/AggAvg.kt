// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorAvg
import org.partiql.types.PType

internal val Agg_AVG__INT8__INT8 = Aggregation.static(
    name = "avg",
    returns = PType.decimal(38, 19),
    parameters = arrayOf(Parameter("value", PType.tinyint())),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__INT16__INT16 = Aggregation.static(
    name = "avg",
    returns = PType.decimal(38, 19),
    parameters = arrayOf(Parameter("value", PType.smallint())),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__INT32__INT32 = Aggregation.static(

    name = "avg",
    returns = PType.decimal(38, 19),
    parameters = arrayOf(
        Parameter("value", PType.integer()),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__INT64__INT64 = Aggregation.static(

    name = "avg",
    returns = PType.decimal(38, 19),
    parameters = arrayOf(
        Parameter("value", PType.bigint()),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__INT__INT = Aggregation.static(

    name = "avg",
    returns = PType.decimal(38, 19),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
    ),
    accumulator = ::AccumulatorAvg,
)

internal val Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Aggregation.static(

    name = "avg",
    returns = PType.decimal(38, 19), // TODO
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
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
    returns = PType.decimal(38, 19),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorAvg,
)
