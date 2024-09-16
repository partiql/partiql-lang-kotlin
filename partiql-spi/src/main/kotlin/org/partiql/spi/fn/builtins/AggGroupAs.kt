// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.Aggregation
import org.partiql.spi.fn.Parameter
import org.partiql.spi.fn.builtins.internal.AccumulatorGroupAs
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
