// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Agg
import org.partiql.planner.internal.fn.AggSignature
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.builtins.internal.AccumulatorEvery
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL

@OptIn(PartiQLValueExperimental::class)
internal object Agg_EVERY__BOOL__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorEvery()
}

@OptIn(PartiQLValueExperimental::class)
internal object Agg_EVERY__ANY__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", ANY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorEvery()
}
