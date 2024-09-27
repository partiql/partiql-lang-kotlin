// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorEvery
import org.partiql.types.PType

internal val Agg_EVERY__BOOL__BOOL = Aggregation.standard(
    name = "every",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.bool()),
    ),
    accumulator = ::AccumulatorEvery,
)

internal val Agg_EVERY__ANY__BOOL = Aggregation.standard(
    name = "every",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorEvery,
)
