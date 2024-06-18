// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorSum
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.DECIMAL_ARBITRARY
import org.partiql.value.PType.Kind.FLOAT32
import org.partiql.value.PType.Kind.FLOAT64
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.SMALLINT
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.BIGINT
import org.partiql.value.PType.Kind.TINYINT


internal object Agg_SUM__TINYINT__TINYINT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = TINYINT,
        parameters = listOf(
            FnParameter("value", TINYINT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum(TINYINT)
}


internal object Agg_SUM__SMALLINT__SMALLINT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = SMALLINT,
        parameters = listOf(
            FnParameter("value", SMALLINT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum(SMALLINT)
}


internal object Agg_SUM__INT__INT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = INT,
        parameters = listOf(
            FnParameter("value", INT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum(INT)
}


internal object Agg_SUM__BIGINT__BIGINT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = BIGINT,
        parameters = listOf(
            FnParameter("value", BIGINT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum(BIGINT)
}


internal object Agg_SUM__NUMERIC__INT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = INT,
        parameters = listOf(
            FnParameter("value", INT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum(INT)
}


internal object Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum(DECIMAL_ARBITRARY)
}


internal object Agg_SUM__FLOAT32__FLOAT32 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = FLOAT32,
        parameters = listOf(
            FnParameter("value", FLOAT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum(FLOAT32)
}


internal object Agg_SUM__FLOAT64__FLOAT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = FLOAT64,
        parameters = listOf(
            FnParameter("value", FLOAT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum(FLOAT64)
}


internal object Agg_SUM__DYNAMIC__DYNAMIC : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = DYNAMIC,
        parameters = listOf(
            FnParameter("value", DYNAMIC),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorSum()
}
