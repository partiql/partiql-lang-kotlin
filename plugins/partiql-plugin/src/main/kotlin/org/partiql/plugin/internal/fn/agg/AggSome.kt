// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.agg

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SOME__BOOL__BOOL : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "some",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BOOL)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation some not implemented")
    }
}
