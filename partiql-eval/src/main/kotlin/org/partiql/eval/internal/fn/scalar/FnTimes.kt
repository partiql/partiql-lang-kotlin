package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTimes0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "times",
        returns = INT8,
        parameters = listOf(FunctionParameter("lhs", INT8), FunctionParameter("rhs", INT8)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTimes1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "times",
        returns = INT16,
        parameters = listOf(FunctionParameter("lhs", INT16), FunctionParameter("rhs", INT16)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTimes2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "times",
        returns = INT32,
        parameters = listOf(FunctionParameter("lhs", INT32), FunctionParameter("rhs", INT32)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTimes3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "times",
        returns = INT64,
        parameters = listOf(FunctionParameter("lhs", INT64), FunctionParameter("rhs", INT64)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTimes4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "times",
        returns = INT,
        parameters = listOf(FunctionParameter("lhs", INT), FunctionParameter("rhs", INT)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTimes5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "times",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FunctionParameter("lhs", DECIMAL_ARBITRARY), FunctionParameter("rhs", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTimes6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "times",
        returns = FLOAT32,
        parameters = listOf(FunctionParameter("lhs", FLOAT32), FunctionParameter("rhs", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTimes7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "times",
        returns = FLOAT64,
        parameters = listOf(FunctionParameter("lhs", FLOAT64), FunctionParameter("rhs", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}


