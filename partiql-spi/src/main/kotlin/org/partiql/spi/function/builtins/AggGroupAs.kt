// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.AggSignature
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorGroupAs
import org.partiql.types.PType

internal object Agg_GROUP_AS__ANY__ANY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "group_as",
        returns = PType.dynamic(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorGroupAs()
}
