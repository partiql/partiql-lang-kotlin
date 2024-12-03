// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.system.builtins.internal.AccumulatorSum
import org.partiql.types.PType

internal val Agg_SUM__INT8__INT8 = Aggregation.static(
    name = "sum",
    returns = PType.tinyint(),
    parameters = arrayOf(
        Parameter("value", PType.tinyint()),
    ),
    accumulator = { AccumulatorSum(PType.tinyint()) },
)

internal val Agg_SUM__INT16__INT16 = Aggregation.static(
    name = "sum",
    returns = PType.smallint(),
    parameters = arrayOf(
        Parameter("value", PType.smallint()),
    ),
    accumulator = { AccumulatorSum(PType.smallint()) },
)

internal val Agg_SUM__INT32__INT32 = Aggregation.static(
    name = "sum",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("value", PType.integer()),
    ),
    accumulator = { AccumulatorSum(PType.integer()) },
)

internal val Agg_SUM__INT64__INT64 = Aggregation.static(
    name = "sum",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("value", PType.bigint())
    ),
    accumulator = { AccumulatorSum(PType.bigint()) },
)

internal val Agg_SUM__NUMERIC__NUMERIC = Aggregation.static(
    name = "sum",
    returns = DefaultNumeric.NUMERIC,
    parameters = arrayOf(
        Parameter("value", DefaultNumeric.NUMERIC),
    ),
    accumulator = { AccumulatorSum(DefaultNumeric.NUMERIC) },
)

internal val Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Aggregation.static(
    name = "sum",
    returns = PType.decimal(38, 19),
    parameters = arrayOf(
        Parameter("value", PType.decimal(38, 19)), // TODO: Rewrite aggregations using new function modeling.
    ),
    accumulator = { AccumulatorSum(PType.decimal(38, 19)) },
)

internal val Agg_SUM__FLOAT32__FLOAT32 = Aggregation.static(
    name = "sum",
    returns = PType.real(),
    parameters = arrayOf(
        Parameter("value", PType.real())
    ),
    accumulator = { AccumulatorSum(PType.real()) },
)

internal val Agg_SUM__FLOAT64__FLOAT64 = Aggregation.static(
    name = "sum",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("value", PType.doublePrecision()),
    ),
    accumulator = { AccumulatorSum(PType.doublePrecision()) },
)

internal val Agg_SUM__ANY__ANY = Aggregation.static(
    name = "sum",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorSum,
)
