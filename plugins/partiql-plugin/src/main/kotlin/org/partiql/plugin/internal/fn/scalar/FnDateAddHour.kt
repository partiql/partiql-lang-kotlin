package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = DATE,
        parameters = listOf(FunctionParameter("interval", INT32), FunctionParameter("datetime", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = DATE,
        parameters = listOf(FunctionParameter("interval", INT64), FunctionParameter("datetime", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = DATE,
        parameters = listOf(FunctionParameter("interval", INT), FunctionParameter("datetime", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(FunctionParameter("interval", INT32), FunctionParameter("datetime", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(FunctionParameter("interval", INT64), FunctionParameter("datetime", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(FunctionParameter("interval", INT), FunctionParameter("datetime", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIMESTAMP,
        parameters = listOf(FunctionParameter("interval", INT32), FunctionParameter("datetime", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIMESTAMP,
        parameters = listOf(FunctionParameter("interval", INT64), FunctionParameter("datetime", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddHour8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIMESTAMP,
        parameters = listOf(FunctionParameter("interval", INT), FunctionParameter("datetime", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_hour not implemented")
    }
}
