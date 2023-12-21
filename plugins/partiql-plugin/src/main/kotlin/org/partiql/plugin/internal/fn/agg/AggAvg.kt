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
internal object AggAvg0 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT8,
        parameters = listOf(FunctionParameter("value", INT8)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggAvg1 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT16,
        parameters = listOf(FunctionParameter("value", INT16)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggAvg2 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT32,
        parameters = listOf(FunctionParameter("value", INT32)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggAvg3 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT64,
        parameters = listOf(FunctionParameter("value", INT64)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggAvg4 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT,
        parameters = listOf(FunctionParameter("value", INT)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggAvg5 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FunctionParameter("value", DECIMAL_ARBITRARY)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggAvg6 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = FLOAT32,
        parameters = listOf(FunctionParameter("value", FLOAT32)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object AggAvg7 : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = FLOAT64,
        parameters = listOf(FunctionParameter("value", FLOAT64)),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}
