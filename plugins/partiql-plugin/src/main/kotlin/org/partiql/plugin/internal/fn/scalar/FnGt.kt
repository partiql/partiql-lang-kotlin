package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT8), FunctionParameter("rhs", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT16), FunctionParameter("rhs", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT32), FunctionParameter("rhs", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT64), FunctionParameter("rhs", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT), FunctionParameter("rhs", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", DECIMAL_ARBITRARY), FunctionParameter("rhs", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", FLOAT32), FunctionParameter("rhs", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", FLOAT64), FunctionParameter("rhs", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", STRING), FunctionParameter("rhs", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt9 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", SYMBOL), FunctionParameter("rhs", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt10 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", CLOB), FunctionParameter("rhs", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt11 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", DATE), FunctionParameter("rhs", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt12 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", TIME), FunctionParameter("rhs", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt13 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", TIMESTAMP), FunctionParameter("rhs", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnGt14 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BOOL), FunctionParameter("rhs", BOOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gt not implemented")
    }
}
