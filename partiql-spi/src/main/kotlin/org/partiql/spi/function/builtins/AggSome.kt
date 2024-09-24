// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.AggSignature
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorAnySome
import org.partiql.types.PType

internal object Agg_SOME__BOOL__BOOL : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "some",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bool()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAnySome()
}

internal object Agg_SOME__ANY__BOOL : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "some",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAnySome()
}
