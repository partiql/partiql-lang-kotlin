// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorCount
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.INT64

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_COUNT__ANY__INT32 : Agg {

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
