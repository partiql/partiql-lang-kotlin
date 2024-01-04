// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.agg

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SUM__INT8__INT8 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "sum",
        returns = INT8,
        parameters = listOf(FunctionParameter("value", INT8)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SUM__INT16__INT16 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "sum",
        returns = INT16,
        parameters = listOf(FunctionParameter("value", INT16)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SUM__INT32__INT32 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "sum",
        returns = INT32,
        parameters = listOf(FunctionParameter("value", INT32)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SUM__INT64__INT64 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "sum",
        returns = INT64,
        parameters = listOf(FunctionParameter("value", INT64)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SUM__INT__INT : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "sum",
        returns = INT,
        parameters = listOf(FunctionParameter("value", INT)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "sum",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FunctionParameter("value", DECIMAL_ARBITRARY)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SUM__FLOAT32__FLOAT32 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "sum",
        returns = FLOAT32,
        parameters = listOf(FunctionParameter("value", FLOAT32)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Agg_SUM__FLOAT64__FLOAT64 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "sum",
        returns = FLOAT64,
        parameters = listOf(FunctionParameter("value", FLOAT64)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}
