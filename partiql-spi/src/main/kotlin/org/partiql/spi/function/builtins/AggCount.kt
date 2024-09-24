// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.AggSignature
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorCount
import org.partiql.types.PType

internal object Agg_COUNT__ANY__INT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "count",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
        ),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorCount()
}
