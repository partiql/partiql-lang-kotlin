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
internal object Fn_UPPER__STRING__STRING : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "upper",
        returns = STRING,
        parameters = listOf(FnParameter("value", STRING),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function upper not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_UPPER__SYMBOL__SYMBOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "upper",
        returns = SYMBOL,
        parameters = listOf(FnParameter("value", SYMBOL),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function upper not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_UPPER__CLOB__CLOB : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "upper",
        returns = CLOB,
        parameters = listOf(FnParameter("value", CLOB),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function upper not implemented")
    }
}


