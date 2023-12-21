package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateDiffYear0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_diff_year",
        returns = INT64,
        parameters = listOf(FunctionParameter("datetime1", DATE), FunctionParameter("datetime2", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_year not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateDiffYear1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_diff_year",
        returns = INT64,
        parameters = listOf(FunctionParameter("datetime1", TIME), FunctionParameter("datetime2", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_year not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateDiffYear2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_diff_year",
        returns = INT64,
        parameters = listOf(FunctionParameter("datetime1", TIMESTAMP), FunctionParameter("datetime2", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_year not implemented")
    }
}
