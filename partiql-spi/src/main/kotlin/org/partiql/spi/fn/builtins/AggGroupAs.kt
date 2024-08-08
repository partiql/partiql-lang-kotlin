// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.builtins.internal.AccumulatorGroupAs
import org.partiql.types.PType

internal object Agg_GROUP_AS__ANY__ANY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "group_as",
        returns = PType.typeDynamic(),
        parameters = listOf(
            FnParameter("value", PType.typeDynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorGroupAs()
}
