// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorSumBigInt
import org.partiql.spi.function.builtins.internal.AccumulatorSumDecimal
import org.partiql.spi.function.builtins.internal.AccumulatorSumDouble
import org.partiql.spi.function.builtins.internal.AccumulatorSumDynamic
import org.partiql.spi.types.PType

/**
 * TODO: This needs to be formalized. See https://github.com/partiql/partiql-lang-kotlin/issues/1659
 * Return types are mostly implementation-defined. Follows what postgresql does for the non-dynamic cases.
 *
 * Return type for tinyint, smalllint, integer -> bigint
 * Return type for bigint, decimal -> decimal
 * Return type for numeric -> numeric
 * Return type for float and double precision -> double precision
 * Return type for dynamic:
 * - if all values are integer or smaller -> bigint
 * - if all values are exact numeric (all integral + decimal/numeric) -> decimal
 * - otherwise -> double precision
 */

internal val Agg_SUM__INT8__INT8 = Aggregation.overload(
    name = "sum",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("value", PType.tinyint()),
    ),
    accumulator = ::AccumulatorSumBigInt
)

internal val Agg_SUM__INT16__INT16 = Aggregation.overload(
    name = "sum",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("value", PType.smallint()),
    ),
    accumulator = ::AccumulatorSumBigInt
)

internal val Agg_SUM__INT32__INT32 = Aggregation.overload(
    name = "sum",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("value", PType.integer()),
    ),
    accumulator = ::AccumulatorSumBigInt
)

internal val Agg_SUM__INT64__INT64 = Aggregation.overload(
    name = "sum",
    returns = DefaultDecimal.DECIMAL,
    parameters = arrayOf(
        Parameter("value", PType.bigint())
    ),
    accumulator = { AccumulatorSumDecimal(DefaultDecimal.DECIMAL) },
)

internal val Agg_SUM__NUMERIC__NUMERIC = Aggregation.overload(
    name = "sum",
    returns = DefaultNumeric.NUMERIC,
    parameters = arrayOf(
        Parameter("value", DefaultNumeric.NUMERIC),
    ),
    accumulator = { AccumulatorSumDecimal(DefaultNumeric.NUMERIC) },
)

internal val Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Aggregation.overload(
    name = "sum",
    returns = DefaultDecimal.DECIMAL,
    parameters = arrayOf(
        Parameter("value", DefaultDecimal.DECIMAL), // TODO: Rewrite aggregations using new function modeling.
    ),
    accumulator = { AccumulatorSumDecimal(DefaultDecimal.DECIMAL) },
)

internal val Agg_SUM__FLOAT32__FLOAT32 = Aggregation.overload(
    name = "sum",
    returns = PType.real(),
    parameters = arrayOf(
        Parameter("value", PType.real())
    ),
    accumulator = { AccumulatorSumDouble() },
)

internal val Agg_SUM__FLOAT64__FLOAT64 = Aggregation.overload(
    name = "sum",
    returns = PType.doublePrecision(),
    parameters = arrayOf(
        Parameter("value", PType.doublePrecision()),
    ),
    accumulator = { AccumulatorSumDouble() },
)

internal val Agg_SUM__ANY__ANY = Aggregation.overload(
    name = "sum",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorSumDynamic,
)
