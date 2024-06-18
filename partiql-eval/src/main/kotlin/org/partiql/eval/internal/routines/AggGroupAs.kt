// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorGroupAs
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PType.Kind


internal object Agg_GROUP_AS__DYNAMIC__DYNAMIC : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "group_as",
        returns = PType.Kind.DYNAMIC,
        parameters = listOf(
            FnParameter("value", PType.Kind.DYNAMIC),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorGroupAs()
}
