// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Agg
import org.partiql.planner.internal.fn.AggSignature
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.builtins.internal.AccumulatorCount
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.INT64

@OptIn(PartiQLValueExperimental::class)
internal object Agg_COUNT__ANY__INT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "count",
        returns = INT64,
        parameters = listOf(
            FnParameter("value", ANY),
        ),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorCount()
}
