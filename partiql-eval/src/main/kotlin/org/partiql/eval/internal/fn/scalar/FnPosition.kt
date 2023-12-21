package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnPosition0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "position",
        returns = INT64,
        parameters = listOf(FunctionParameter("probe", STRING), FunctionParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function position not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnPosition1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "position",
        returns = INT64,
        parameters = listOf(FunctionParameter("probe", SYMBOL), FunctionParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function position not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnPosition2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "position",
        returns = INT64,
        parameters = listOf(FunctionParameter("probe", CLOB), FunctionParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function position not implemented")
    }
}


