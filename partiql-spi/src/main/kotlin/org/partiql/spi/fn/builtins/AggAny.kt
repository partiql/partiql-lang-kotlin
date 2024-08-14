// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.builtins.internal.AccumulatorAnySome
import org.partiql.types.PType

internal object Agg_ANY__BOOL__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "any",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bool()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAnySome()
}

internal object Agg_ANY__ANY__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "any",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAnySome()
}
