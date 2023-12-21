package org.partiql.eval.internal.fn.agg


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggCount : PartiQLFunction.Aggregation {

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


