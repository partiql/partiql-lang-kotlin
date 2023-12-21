package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
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
internal object FnDivide0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "divide",
        returns = INT8,
        parameters = listOf(FunctionParameter("lhs", INT8), FunctionParameter("rhs", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function divide not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDivide1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "divide",
        returns = INT16,
        parameters = listOf(FunctionParameter("lhs", INT16), FunctionParameter("rhs", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function divide not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDivide2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "divide",
        returns = INT32,
        parameters = listOf(FunctionParameter("lhs", INT32), FunctionParameter("rhs", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function divide not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDivide3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "divide",
        returns = INT64,
        parameters = listOf(FunctionParameter("lhs", INT64), FunctionParameter("rhs", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function divide not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDivide4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "divide",
        returns = INT,
        parameters = listOf(FunctionParameter("lhs", INT), FunctionParameter("rhs", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function divide not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDivide5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "divide",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FunctionParameter("lhs", DECIMAL_ARBITRARY), FunctionParameter("rhs", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function divide not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDivide6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "divide",
        returns = FLOAT32,
        parameters = listOf(FunctionParameter("lhs", FLOAT32), FunctionParameter("rhs", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function divide not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDivide7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "divide",
        returns = FLOAT64,
        parameters = listOf(FunctionParameter("lhs", FLOAT64), FunctionParameter("rhs", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function divide not implemented")
    }
}
