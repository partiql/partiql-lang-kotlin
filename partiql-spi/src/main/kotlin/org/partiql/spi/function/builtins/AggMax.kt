// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorMax
import org.partiql.types.PType

internal val Agg_MAX__INT8__INT8 = Aggregation.standard(

    name = "max",
    returns = PType.tinyint(),
    parameters = arrayOf(
        Parameter("value", PType.tinyint()),
    ),
    accumulator = ::AccumulatorMax,
)

internal val Agg_MAX__INT16__INT16 = Aggregation.standard(

    name = "max",
    returns = PType.smallint(),
    parameters = arrayOf(
        Parameter("value", PType.smallint()),
    ),
    accumulator = ::AccumulatorMax,
)

internal val Agg_MAX__INT32__INT32 = Aggregation.standard(

    name = "max",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("value", PType.integer()),
    ),
    accumulator = ::AccumulatorMax,
)

internal val Agg_MAX__INT64__INT64 = Aggregation.standard(

    name = "max",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("value", PType.bigint()),
    ),
    accumulator = ::AccumulatorMax,
)

internal val Agg_MAX__INT__INT = Aggregation.standard(

    name = "max",
    returns = PType.numeric(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
    ),
    accumulator = ::AccumulatorMax,
)

internal val Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Aggregation.standard(

    name = "max",
    returns = PType.decimal(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
    ),

    accumulator = ::AccumulatorMax,
)

internal val Agg_MAX__FLOAT32__FLOAT32 = Aggregation.standard(

    name = "max",
    returns = PType.real(),
    parameters = arrayOf(
        Parameter("value", PType.real()),
    ),
    accumulator = ::AccumulatorMax,
)

internal val Agg_MAX__FLOAT64__FLOAT64 = Aggregation.standard(

    name = "max",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("value", PType.doublePrecision()),
    ),
    accumulator = ::AccumulatorMax,
)

internal val Agg_MAX__ANY__ANY = Aggregation.standard(

    name = "max",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorMax,
)
