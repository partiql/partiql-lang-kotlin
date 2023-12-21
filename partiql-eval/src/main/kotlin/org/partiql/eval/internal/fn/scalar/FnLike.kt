package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLike0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", STRING), FunctionParameter("pattern", STRING)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLike1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", SYMBOL), FunctionParameter("pattern", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLike2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", CLOB), FunctionParameter("pattern", CLOB)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like not implemented")
    }
}


