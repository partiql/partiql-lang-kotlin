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
internal object Fn_TRIM_CHARS__STRING_STRING__STRING : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "trim_chars",
        returns = STRING,
        parameters = listOf(FnParameter("value", STRING),
FnParameter("chars", STRING),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_chars not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_CHARS__SYMBOL_SYMBOL__SYMBOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "trim_chars",
        returns = SYMBOL,
        parameters = listOf(FnParameter("value", SYMBOL),
FnParameter("chars", SYMBOL),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_chars not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_CHARS__CLOB_CLOB__CLOB : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "trim_chars",
        returns = CLOB,
        parameters = listOf(FnParameter("value", CLOB),
FnParameter("chars", CLOB),),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_chars not implemented")
    }
}


