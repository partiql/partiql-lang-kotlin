// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorMin
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


internal object Agg_MIN__TINYINT__TINYINT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = TINYINT,
        parameters = listOf(
            FnParameter("value", TINYINT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}


internal object Agg_MIN__SMALLINT__SMALLINT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = SMALLINT,
        parameters = listOf(
            FnParameter("value", SMALLINT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}


internal object Agg_MIN__INT__INT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = INT,
        parameters = listOf(
            FnParameter("value", INT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}


internal object Agg_MIN__BIGINT__BIGINT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = BIGINT,
        parameters = listOf(
            FnParameter("value", BIGINT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}


internal object Agg_MIN__NUMERIC__INT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = INT,
        parameters = listOf(
            FnParameter("value", INT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}


internal object Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}


internal object Agg_MIN__FLOAT32__FLOAT32 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = FLOAT32,
        parameters = listOf(
            FnParameter("value", FLOAT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}


internal object Agg_MIN__FLOAT64__FLOAT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = FLOAT64,
        parameters = listOf(
            FnParameter("value", FLOAT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}


internal object Agg_MIN__DYNAMIC__DYNAMIC : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "min",
        returns = DYNAMIC,
        parameters = listOf(
            FnParameter("value", DYNAMIC),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorMin()
}
