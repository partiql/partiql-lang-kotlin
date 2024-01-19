// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NOT__BOOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "not",
        returns = BOOL,
        parameters = listOf(FnParameter("value", BOOL),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function not not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NOT__MISSING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "not",
        returns = BOOL,
        parameters = listOf(FnParameter("value", MISSING),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function not not implemented")
    }
}
