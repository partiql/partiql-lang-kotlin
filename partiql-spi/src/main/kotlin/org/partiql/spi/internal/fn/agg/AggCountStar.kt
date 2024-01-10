// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.internal.fn.agg

import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_COUNT_STAR____INT32 : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "count_star",
        returns = INT32,
        parameters = listOf(),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation count_star not implemented")
    }
}
