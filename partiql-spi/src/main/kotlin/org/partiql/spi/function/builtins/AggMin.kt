// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorMin
import org.partiql.types.PType

internal val Agg_MIN__INT8__INT8 = Aggregation.static(
    name = "min",
    returns = PType.tinyint(),
    parameters = arrayOf(
        Parameter("value", PType.tinyint()),
    ),
    accumulator = ::AccumulatorMin,
)

internal val Agg_MIN__INT16__INT16 = Aggregation.static(
    name = "min",
    returns = PType.smallint(),
    parameters = arrayOf(
        Parameter("value", PType.smallint()),
    ),
    accumulator = ::AccumulatorMin,
)

internal val Agg_MIN__INT32__INT32 = Aggregation.static(
    name = "min",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("value", PType.integer()),
    ),
    accumulator = ::AccumulatorMin,
)

internal val Agg_MIN__INT64__INT64 = Aggregation.static(
    name = "min",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("value", PType.bigint()),
    ),
    accumulator = ::AccumulatorMin,
)

internal val Agg_MIN__INT__INT = Aggregation.static(
    name = "min",
    returns = DefaultNumeric.NUMERIC,
    parameters = arrayOf(
        Parameter("value", DefaultNumeric.NUMERIC), // TODO: Rewrite aggregations using new function modeling.
    ),
    accumulator = ::AccumulatorMin,
)

internal val Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Aggregation.static(
    name = "min",
    returns = PType.decimal(38, 19),
    parameters = arrayOf(
        Parameter("value", PType.decimal(38, 19)), // TODO: Rewrite aggregations using new function modeling.
    ),
    accumulator = ::AccumulatorMin,
)

internal val Agg_MIN__FLOAT32__FLOAT32 = Aggregation.static(
    name = "min",
    returns = PType.real(),
    parameters = arrayOf(
        Parameter("value", PType.real()),
    ),
    accumulator = ::AccumulatorMin,
)

internal val Agg_MIN__FLOAT64__FLOAT64 = Aggregation.static(
    name = "min",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("value", PType.doublePrecision()),
    ),
    accumulator = ::AccumulatorMin,
)

internal val Agg_MIN__ANY__ANY = Aggregation.static(
    name = "min",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorMin,
)
