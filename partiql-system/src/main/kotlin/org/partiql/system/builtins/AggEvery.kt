// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.system.builtins.internal.AccumulatorEvery
import org.partiql.types.PType

internal val Agg_EVERY__BOOL__BOOL = Aggregation.static(
    name = "every",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.bool()),
    ),
    accumulator = ::AccumulatorEvery,
)

internal val Agg_EVERY__ANY__BOOL = Aggregation.static(
    name = "every",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorEvery,
)
