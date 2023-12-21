package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT8), FunctionParameter("rhs", INT8)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT16), FunctionParameter("rhs", INT16)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT32), FunctionParameter("rhs", INT32)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT64), FunctionParameter("rhs", INT64)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT), FunctionParameter("rhs", INT)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", DECIMAL_ARBITRARY), FunctionParameter("rhs", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", FLOAT32), FunctionParameter("rhs", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", FLOAT64), FunctionParameter("rhs", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", STRING), FunctionParameter("rhs", STRING)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt9 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", SYMBOL), FunctionParameter("rhs", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt10 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", CLOB), FunctionParameter("rhs", CLOB)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt11 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", DATE), FunctionParameter("rhs", DATE)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt12 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", TIME), FunctionParameter("rhs", TIME)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt13 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", TIMESTAMP), FunctionParameter("rhs", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLt14 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "lt",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BOOL), FunctionParameter("rhs", BOOL)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lt not implemented")
    }
}


