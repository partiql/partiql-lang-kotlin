// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.sql.builtins

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorAvg
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__INT8__INT8 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = DECIMAL,
        parameters = listOf(
            FnParameter("value", INT8),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(DECIMAL)
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__INT16__INT16 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = DECIMAL,
        parameters = listOf(
            FnParameter("value", INT16),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(DECIMAL)
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__INT32__INT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = DECIMAL,
        parameters = listOf(
            FnParameter("value", INT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(DECIMAL)
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__INT64__INT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = DECIMAL,
        parameters = listOf(
            FnParameter("value", INT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(DECIMAL)
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__INT__INT : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("value", INT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(DECIMAL_ARBITRARY)
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(DECIMAL_ARBITRARY)
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__FLOAT32__FLOAT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = FLOAT32,
        parameters = listOf(
            FnParameter("value", FLOAT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(FLOAT32)
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__FLOAT64__FLOAT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = FLOAT64,
        parameters = listOf(
            FnParameter("value", FLOAT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(FLOAT64)
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
public object Agg_AVG__ANY__ANY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("value", ANY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(DECIMAL_ARBITRARY)
}
