// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.boolValue
import org.partiql.value.check

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
        val value = args[0].check<BoolValue>().value!!
        return boolValue(value.not())
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

    // TODO: determine what this behavior should be
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        throw TypeCheckException()
    }
}
