// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorGroupAs
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_GROUP_AS__ANY__ANY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "group_as",
        returns = PartiQLValueType.ANY,
        parameters = listOf(
            FnParameter("value", PartiQLValueType.ANY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorGroupAs()
}
