// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.AggSignature
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorMin
import org.partiql.types.PType

internal object Agg_MIN__INT8__INT8 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.tinyint(),
        parameters = listOf(
            Parameter("value", PType.tinyint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}

internal object Agg_MIN__INT16__INT16 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.smallint(),
        parameters = listOf(
            Parameter("value", PType.smallint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}

internal object Agg_MIN__INT32__INT32 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("value", PType.integer()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}

internal object Agg_MIN__INT64__INT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("value", PType.bigint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}

internal object Agg_MIN__INT__INT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.numeric(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}

internal object Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}

internal object Agg_MIN__FLOAT32__FLOAT32 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.real(),
        parameters = listOf(
            Parameter("value", PType.real()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}

internal object Agg_MIN__FLOAT64__FLOAT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.doublePrecision(),
        parameters = listOf(
            Parameter("value", PType.doublePrecision()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}

internal object Agg_MIN__ANY__ANY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = PType.dynamic(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMin()
}
