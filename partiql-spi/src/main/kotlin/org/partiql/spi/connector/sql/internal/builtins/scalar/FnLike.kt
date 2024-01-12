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
internal object Fn_LIKE__STRING_STRING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(FnParameter("value", STRING),
FnParameter("pattern", STRING),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LIKE__SYMBOL_SYMBOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(FnParameter("value", SYMBOL),
FnParameter("pattern", SYMBOL),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_LIKE__CLOB_CLOB__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(FnParameter("value", CLOB),
FnParameter("pattern", CLOB),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like not implemented")
    }
}


