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
internal object Fn_NEG__INT8__INT8 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "neg",
        returns = INT8,
        parameters = listOf(FnParameter("value", INT8),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT16__INT16 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "neg",
        returns = INT16,
        parameters = listOf(FnParameter("value", INT16),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT32__INT32 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "neg",
        returns = INT32,
        parameters = listOf(FnParameter("value", INT32),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT64__INT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "neg",
        returns = INT64,
        parameters = listOf(FnParameter("value", INT64),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT__INT : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "neg",
        returns = INT,
        parameters = listOf(FnParameter("value", INT),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "neg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("value", DECIMAL_ARBITRARY),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__FLOAT32__FLOAT32 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "neg",
        returns = FLOAT32,
        parameters = listOf(FnParameter("value", FLOAT32),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__FLOAT64__FLOAT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "neg",
        returns = FLOAT64,
        parameters = listOf(FnParameter("value", FLOAT64),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}


