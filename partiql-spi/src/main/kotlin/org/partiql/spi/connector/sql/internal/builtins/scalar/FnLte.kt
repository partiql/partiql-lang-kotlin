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
internal object Fn_LTE__INT8_INT8__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", INT8),
FnParameter("rhs", INT8),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__INT16_INT16__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", INT16),
FnParameter("rhs", INT16),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__INT32_INT32__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", INT32),
FnParameter("rhs", INT32),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__INT64_INT64__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", INT64),
FnParameter("rhs", INT64),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__INT_INT__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", INT),
FnParameter("rhs", INT),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", DECIMAL_ARBITRARY),
FnParameter("rhs", DECIMAL_ARBITRARY),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__FLOAT32_FLOAT32__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", FLOAT32),
FnParameter("rhs", FLOAT32),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__FLOAT64_FLOAT64__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", FLOAT64),
FnParameter("rhs", FLOAT64),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__STRING_STRING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", STRING),
FnParameter("rhs", STRING),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__SYMBOL_SYMBOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", SYMBOL),
FnParameter("rhs", SYMBOL),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__CLOB_CLOB__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", CLOB),
FnParameter("rhs", CLOB),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__DATE_DATE__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", DATE),
FnParameter("rhs", DATE),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__TIME_TIME__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", TIME),
FnParameter("rhs", TIME),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__TIMESTAMP_TIMESTAMP__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", TIMESTAMP),
FnParameter("rhs", TIMESTAMP),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LTE__BOOL_BOOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "lte",
        returns = BOOL,
        parameters = listOf(FnParameter("lhs", BOOL),
FnParameter("rhs", BOOL),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function lte not implemented")
    }
}


