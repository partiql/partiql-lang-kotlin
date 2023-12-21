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
internal object AggSum0 : PartiQLFunction.Aggregation {

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
internal object AggSum1 : PartiQLFunction.Aggregation {

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
internal object AggSum2 : PartiQLFunction.Aggregation {

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
internal object AggSum3 : PartiQLFunction.Aggregation {

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
internal object AggSum4 : PartiQLFunction.Aggregation {

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
internal object AggSum5 : PartiQLFunction.Aggregation {

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
internal object AggSum6 : PartiQLFunction.Aggregation {

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
internal object AggSum7 : PartiQLFunction.Aggregation {

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
