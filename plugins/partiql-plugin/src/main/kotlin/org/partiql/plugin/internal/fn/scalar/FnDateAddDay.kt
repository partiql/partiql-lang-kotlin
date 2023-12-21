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
internal object FnDateAddDay0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = DATE,
        parameters = listOf(FunctionParameter("interval", INT32), FunctionParameter("datetime", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddDay1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = DATE,
        parameters = listOf(FunctionParameter("interval", INT64), FunctionParameter("datetime", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddDay2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = DATE,
        parameters = listOf(FunctionParameter("interval", INT), FunctionParameter("datetime", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddDay3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = TIME,
        parameters = listOf(FunctionParameter("interval", INT32), FunctionParameter("datetime", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddDay4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = TIME,
        parameters = listOf(FunctionParameter("interval", INT64), FunctionParameter("datetime", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddDay5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = TIME,
        parameters = listOf(FunctionParameter("interval", INT), FunctionParameter("datetime", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddDay6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = TIMESTAMP,
        parameters = listOf(FunctionParameter("interval", INT32), FunctionParameter("datetime", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddDay7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = TIMESTAMP,
        parameters = listOf(FunctionParameter("interval", INT64), FunctionParameter("datetime", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateAddDay8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_day",
        returns = TIMESTAMP,
        parameters = listOf(FunctionParameter("interval", INT), FunctionParameter("datetime", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_add_day not implemented")
    }
}
