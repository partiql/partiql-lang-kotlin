// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorAvgDecimal
import org.partiql.spi.function.builtins.internal.AccumulatorAvgDouble
import org.partiql.spi.function.builtins.internal.AccumulatorAvgDynamic
import org.partiql.spi.types.PType

/**
 * TODO: This needs to be formalized. See https://github.com/partiql/partiql-lang-kotlin/issues/1659
 * Return types are mostly implementation-defined. Follows what postgresql does for the non-dynamic cases.
 *
 * Return type for tinyint, smallint, integer, bigint, decimal, numeric -> decimal
 * Return type for float and double precision -> double precision
 * Return type for dynamic:
 * - if all values are exact numeric -> decimal
 * - otherwise -> double precision
 */
private val AVG_DECIMAL = DefaultDecimal.DECIMAL

internal val Agg_AVG__INT8__INT8 = Aggregation.overload(
    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(Parameter("value", PType.tinyint())),
    accumulator = ::AccumulatorAvgDecimal,
)

internal val Agg_AVG__INT16__INT16 = Aggregation.overload(
    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(Parameter("value", PType.smallint())),
    accumulator = ::AccumulatorAvgDecimal,
)

internal val Agg_AVG__INT32__INT32 = Aggregation.overload(

    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(
        Parameter("value", PType.integer()),
    ),
    accumulator = ::AccumulatorAvgDecimal,
)

internal val Agg_AVG__INT64__INT64 = Aggregation.overload(

    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(
        Parameter("value", PType.bigint()),
    ),
    accumulator = ::AccumulatorAvgDecimal,
)

internal val Agg_AVG__NUMERIC__NUMERIC = Aggregation.overload(

    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(
        Parameter("value", DefaultNumeric.NUMERIC),
    ),
    accumulator = ::AccumulatorAvgDecimal,
)

internal val Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Aggregation.overload(

    name = "avg",
    returns = AVG_DECIMAL,
    parameters = arrayOf(
        Parameter("value", AVG_DECIMAL),
    ),
    accumulator = ::AccumulatorAvgDecimal,
)

internal val Agg_AVG__FLOAT32__FLOAT32 = Aggregation.overload(

    name = "avg",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("value", PType.real()),
    ),
    accumulator = ::AccumulatorAvgDouble,
)

internal val Agg_AVG__FLOAT64__FLOAT64 = Aggregation.overload(

    name = "avg",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("value", PType.doublePrecision()),
    ),
    accumulator = ::AccumulatorAvgDouble,
)

internal val Agg_AVG__ANY__ANY = Aggregation.overload(

    name = "avg",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorAvgDynamic,
)
