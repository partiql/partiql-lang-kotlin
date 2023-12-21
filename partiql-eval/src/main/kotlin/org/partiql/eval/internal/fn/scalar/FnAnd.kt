package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnAnd0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "and",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BOOL), FunctionParameter("rhs", BOOL)),
        isNullCall = false,
        isNullable = true,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function and not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnAnd1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "and",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", MISSING), FunctionParameter("rhs", BOOL)),
        isNullCall = false,
        isNullable = true,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function and not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnAnd2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "and",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BOOL), FunctionParameter("rhs", MISSING)),
        isNullCall = false,
        isNullable = true,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function and not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnAnd3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "and",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", MISSING), FunctionParameter("rhs", MISSING)),
        isNullCall = false,
        isNullable = true,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function and not implemented")
    }
}


