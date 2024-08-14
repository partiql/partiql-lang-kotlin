// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.builtins.internal.AccumulatorMax
import org.partiql.types.PType

internal object Agg_MAX__INT8__INT8 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.tinyint(),
        parameters = listOf(
            FnParameter("value", PType.tinyint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT16__INT16 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.smallint(),
        parameters = listOf(
            FnParameter("value", PType.smallint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT32__INT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.integer(),
        parameters = listOf(
            FnParameter("value", PType.integer()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT64__INT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("value", PType.bigint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT__INT : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.numeric(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.numeric()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.decimal()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__FLOAT32__FLOAT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.real(),
        parameters = listOf(
            FnParameter("value", PType.real()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__FLOAT64__FLOAT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.doublePrecision(),
        parameters = listOf(
            FnParameter("value", PType.doublePrecision()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__ANY__ANY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.dynamic(),
        parameters = listOf(
            FnParameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}
