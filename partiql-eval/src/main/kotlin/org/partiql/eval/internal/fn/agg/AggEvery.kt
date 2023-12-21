package org.partiql.eval.internal.fn.agg


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggEvery : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "every",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BOOL)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation every not implemented")
    }
}


