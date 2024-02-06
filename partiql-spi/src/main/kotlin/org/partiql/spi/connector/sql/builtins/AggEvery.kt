// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_EVERY__BOOL__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation every not implemented")
    }
}
