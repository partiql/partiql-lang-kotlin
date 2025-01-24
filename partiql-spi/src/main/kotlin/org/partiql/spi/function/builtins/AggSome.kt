// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorAnySome
import org.partiql.spi.types.PType

internal val Agg_SOME__BOOL__BOOL = Aggregation.overload(

    name = "some",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.bool()),
    ),
    accumulator = ::AccumulatorAnySome,
)

internal val Agg_SOME__ANY__BOOL = Aggregation.overload(

    name = "some",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorAnySome,
)
