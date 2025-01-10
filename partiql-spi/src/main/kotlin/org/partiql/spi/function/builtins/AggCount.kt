// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorCount
import org.partiql.spi.types.PType

internal val Agg_COUNT__ANY__INT64 = Aggregation.static(
    name = "count",
    returns = PType.bigint(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),
    accumulator = ::AccumulatorCount,
)
