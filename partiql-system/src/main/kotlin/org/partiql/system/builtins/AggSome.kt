// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.system.builtins.internal.AccumulatorAnySome
import org.partiql.types.PType

internal val Agg_SOME__BOOL__BOOL = Aggregation.static(

    name = "some",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.bool()),
    ),
    accumulator = ::AccumulatorAnySome,
)

internal val Agg_SOME__ANY__BOOL = Aggregation.static(

    name = "some",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorAnySome,
)
