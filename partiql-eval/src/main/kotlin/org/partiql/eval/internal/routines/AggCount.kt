// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorCount
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BIGINT


internal object Agg_COUNT__DYNAMIC__BIGINT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "count",
        returns = BIGINT,
        parameters = listOf(
            FnParameter("value", DYNAMIC),
        ),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorCount()
}
