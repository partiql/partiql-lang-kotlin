// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.agg

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_COUNT__ANY__INT32 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "count",
        returns = INT32,
        parameters = listOf(FunctionParameter("value", ANY)),
        isNullable = false,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation count not implemented")
    }
}
