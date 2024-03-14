// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NOT__BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "not",
        returns = BOOL,
        parameters = listOf(FnParameter("value", BOOL)),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
        isMissingCall = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<BoolValue>().value!!
        return boolValue(value.not())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NOT__MISSING__BOOL : Fn {

    override val signature = FnSignature(
        name = "not",
        returns = MISSING,
        parameters = listOf(FnParameter("value", MISSING)),
        isNullable = false,
        isNullCall = true,
        isMissable = true,
        isMissingCall = true,
    )

    // TODO: determine what this behavior should be
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        throw TypeCheckException()
    }
}
