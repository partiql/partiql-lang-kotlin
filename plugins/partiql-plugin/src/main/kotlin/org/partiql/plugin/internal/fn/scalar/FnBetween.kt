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
internal object FnBetween0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT8),
            FunctionParameter("lower", INT8),
            FunctionParameter("upper", INT8)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT16),
            FunctionParameter("lower", INT16),
            FunctionParameter("upper", INT16)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT32),
            FunctionParameter("lower", INT32),
            FunctionParameter("upper", INT32)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT64),
            FunctionParameter("lower", INT64),
            FunctionParameter("upper", INT64)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT),
            FunctionParameter("lower", INT),
            FunctionParameter("upper", INT)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DECIMAL_ARBITRARY),
            FunctionParameter("lower", DECIMAL_ARBITRARY),
            FunctionParameter("upper", DECIMAL_ARBITRARY)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT32),
            FunctionParameter("lower", FLOAT32),
            FunctionParameter("upper", FLOAT32)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT64),
            FunctionParameter("lower", FLOAT64),
            FunctionParameter("upper", FLOAT64)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRING),
            FunctionParameter("lower", STRING),
            FunctionParameter("upper", STRING)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween9 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SYMBOL),
            FunctionParameter("lower", SYMBOL),
            FunctionParameter("upper", SYMBOL)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween10 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CLOB),
            FunctionParameter("lower", CLOB),
            FunctionParameter("upper", CLOB)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween11 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DATE),
            FunctionParameter("lower", DATE),
            FunctionParameter("upper", DATE)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween12 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIME),
            FunctionParameter("lower", TIME),
            FunctionParameter("upper", TIME)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnBetween13 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIMESTAMP),
            FunctionParameter("lower", TIMESTAMP),
            FunctionParameter("upper", TIMESTAMP)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}
