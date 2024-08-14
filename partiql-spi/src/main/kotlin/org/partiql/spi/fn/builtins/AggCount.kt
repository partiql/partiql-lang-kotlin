// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.builtins.internal.AccumulatorCount
import org.partiql.types.PType

internal object Agg_COUNT__ANY__INT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "count",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("value", PType.dynamic()),
        ),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorCount()
}
