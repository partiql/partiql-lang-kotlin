// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.sql.builtins

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorAnySome
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_SOME__BOOL__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "some",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAnySome()
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_SOME__ANY__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "some",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", ANY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAnySome()
}
