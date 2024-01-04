// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.agg

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_COUNT_STAR____INT32 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "count_star",
        returns = INT32,
        parameters = listOf(),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation count_star not implemented")
    }
}
