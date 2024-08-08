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
        returns = PType.typeTinyInt(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT16__INT16 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.typeSmallInt(),
        parameters = listOf(
            FnParameter("value", PType.typeSmallInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT32__INT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("value", PType.typeInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT64__INT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.typeBigInt(),
        parameters = listOf(
            FnParameter("value", PType.typeBigInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT__INT : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.typeIntArbitrary(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeIntArbitrary()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeDecimalArbitrary()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__FLOAT32__FLOAT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.typeReal(),
        parameters = listOf(
            FnParameter("value", PType.typeReal()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__FLOAT64__FLOAT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.typeDoublePrecision(),
        parameters = listOf(
            FnParameter("value", PType.typeDoublePrecision()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__ANY__ANY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.typeDynamic(),
        parameters = listOf(
            FnParameter("value", PType.typeDynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorMax()
}
