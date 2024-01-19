// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.agg

import org.partiql.spi.fn.FnAggregation
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_COUNT_STAR____INT32 : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "count_star",
        returns = INT32,
        parameters = listOf(),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation count_star not implemented")
    }
}
