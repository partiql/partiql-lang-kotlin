// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar


import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.*


@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TIMES__INT8_INT8__INT8 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "times",
        returns = INT8,
        parameters = listOf(FnParameter("lhs", INT8),
FnParameter("rhs", INT8),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TIMES__INT16_INT16__INT16 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "times",
        returns = INT16,
        parameters = listOf(FnParameter("lhs", INT16),
FnParameter("rhs", INT16),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TIMES__INT32_INT32__INT32 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "times",
        returns = INT32,
        parameters = listOf(FnParameter("lhs", INT32),
FnParameter("rhs", INT32),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TIMES__INT64_INT64__INT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "times",
        returns = INT64,
        parameters = listOf(FnParameter("lhs", INT64),
FnParameter("rhs", INT64),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TIMES__INT_INT__INT : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "times",
        returns = INT,
        parameters = listOf(FnParameter("lhs", INT),
FnParameter("rhs", INT),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TIMES__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "times",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("lhs", DECIMAL_ARBITRARY),
FnParameter("rhs", DECIMAL_ARBITRARY),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TIMES__FLOAT32_FLOAT32__FLOAT32 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "times",
        returns = FLOAT32,
        parameters = listOf(FnParameter("lhs", FLOAT32),
FnParameter("rhs", FLOAT32),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TIMES__FLOAT64_FLOAT64__FLOAT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "times",
        returns = FLOAT64,
        parameters = listOf(FnParameter("lhs", FLOAT64),
FnParameter("rhs", FLOAT64),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function times not implemented")
    }
}


