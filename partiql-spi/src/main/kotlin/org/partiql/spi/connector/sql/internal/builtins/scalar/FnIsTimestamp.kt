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
internal object Fn_IS_TIMESTAMP__ANY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "is_timestamp",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY),),
        isNullCall = false,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_timestamp not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_TIMESTAMP__BOOL_INT32_ANY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "is_timestamp",
        returns = BOOL,
        parameters = listOf(FnParameter("type_parameter_1", BOOL),
FnParameter("type_parameter_2", INT32),
FnParameter("value", ANY),),
        isNullCall = false,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_timestamp not implemented")
    }
}


