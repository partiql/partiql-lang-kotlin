// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.internal.fn.agg

import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_COUNT__ANY__INT32 : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "count",
        returns = INT32,
        parameters = listOf(
            FunctionParameter("value", ANY),
        ),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation count not implemented")
    }
}
