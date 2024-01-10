// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.internal.fn.agg

import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_SOME__BOOL__BOOL : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "some",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BOOL),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation some not implemented")
    }
}
