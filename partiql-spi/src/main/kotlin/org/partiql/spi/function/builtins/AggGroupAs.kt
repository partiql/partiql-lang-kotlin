// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorGroupAs
import org.partiql.spi.types.PType

internal val Agg_GROUP_AS__ANY__ANY = Aggregation.overload(
    name = "group_as",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
    ),
    accumulator = ::AccumulatorGroupAs,
)
