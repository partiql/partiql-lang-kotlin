// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorSum
import org.partiql.types.PType

internal val Agg_SUM__INT8__INT8 = Aggregation.standard(
    name = "sum",
    returns = PType.tinyint(),
    parameters = arrayOf(
        Parameter("value", PType.tinyint()),
    ),
    accumulator = { AccumulatorSum(PType.tinyint()) },
)

internal val Agg_SUM__INT16__INT16 = Aggregation.standard(
    name = "sum",
    returns = PType.smallint(),
    parameters = arrayOf(
        Parameter("value", PType.smallint()),
    ),
    accumulator = { AccumulatorSum(PType.smallint()) },
)

internal val Agg_SUM__INT32__INT32 = Aggregation.standard(
    name = "sum",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("value", PType.integer()),
    ),
    accumulator = { AccumulatorSum(PType.integer()) },
)

internal val Agg_SUM__INT64__INT64 = Aggregation.standard(
    name = "sum",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("value", PType.bigint())
    ),
    accumulator = { AccumulatorSum(PType.bigint()) },
)

internal val Agg_SUM__INT__INT = Aggregation.standard(
    name = "sum",
    returns = PType.numeric(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
    ),
    accumulator = { AccumulatorSum(PType.numeric()) },
)

internal val Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Aggregation.standard(
    name = "sum",
    returns = PType.decimal(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
    ),
    accumulator = { AccumulatorSum(PType.decimal()) },
)

internal val Agg_SUM__FLOAT32__FLOAT32 = Aggregation.standard(
    name = "sum",
    returns = PType.real(),
    parameters = arrayOf(
        Parameter("value", PType.real())
    ),
    accumulator = { AccumulatorSum(PType.real()) },
)

internal val Agg_SUM__FLOAT64__FLOAT64 = Aggregation.standard(
    name = "sum",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("value", PType.doublePrecision()),
    ),
    accumulator = { AccumulatorSum(PType.doublePrecision()) },
)

internal val Agg_SUM__ANY__ANY = Aggregation.standard(
    name = "sum",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorSum,
)
