// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.agg

import org.partiql.spi.fn.FnAggregation
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.*

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_COUNT__ANY__INT32 : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "count",
        returns = INT32,
        parameters = listOf(
            FnParameter("value", ANY),
        ),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation count not implemented")
    }
}
