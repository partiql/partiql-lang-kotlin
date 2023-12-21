package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnMinus0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "minus",
        returns = INT8,
        parameters = listOf(FunctionParameter("lhs", INT8), FunctionParameter("rhs", INT8)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function minus not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnMinus1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "minus",
        returns = INT16,
        parameters = listOf(FunctionParameter("lhs", INT16), FunctionParameter("rhs", INT16)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function minus not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnMinus2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "minus",
        returns = INT32,
        parameters = listOf(FunctionParameter("lhs", INT32), FunctionParameter("rhs", INT32)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function minus not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnMinus3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "minus",
        returns = INT64,
        parameters = listOf(FunctionParameter("lhs", INT64), FunctionParameter("rhs", INT64)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function minus not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnMinus4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "minus",
        returns = INT,
        parameters = listOf(FunctionParameter("lhs", INT), FunctionParameter("rhs", INT)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function minus not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnMinus5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "minus",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FunctionParameter("lhs", DECIMAL_ARBITRARY), FunctionParameter("rhs", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function minus not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnMinus6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "minus",
        returns = FLOAT32,
        parameters = listOf(FunctionParameter("lhs", FLOAT32), FunctionParameter("rhs", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function minus not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnMinus7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "minus",
        returns = FLOAT64,
        parameters = listOf(FunctionParameter("lhs", FLOAT64), FunctionParameter("rhs", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function minus not implemented")
    }
}


