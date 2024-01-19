// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.agg

import org.partiql.spi.fn.FnAggregation
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_EVERY__BOOL__BOOL : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "every",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation every not implemented")
    }
}
