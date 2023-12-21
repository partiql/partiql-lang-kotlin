package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateDiffMonth0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_diff_month",
        returns = INT64,
        parameters = listOf(FunctionParameter("datetime1", DATE), FunctionParameter("datetime2", DATE)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_month not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateDiffMonth1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_diff_month",
        returns = INT64,
        parameters = listOf(FunctionParameter("datetime1", TIME), FunctionParameter("datetime2", TIME)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_month not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnDateDiffMonth2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_diff_month",
        returns = INT64,
        parameters = listOf(FunctionParameter("datetime1", TIMESTAMP), FunctionParameter("datetime2", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function date_diff_month not implemented")
    }
}


