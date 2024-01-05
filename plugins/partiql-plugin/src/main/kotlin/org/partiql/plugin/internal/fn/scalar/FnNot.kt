// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NOT__BOOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "not",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BOOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<BoolValue>().value
        return boolValue(value!!.not())
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NOT__MISSING__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "not",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", MISSING)),
        isNullCall = true,
        isNullable = true,
    )

    // TODO determine what this behavior should be
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        throw TypeCheckException()
    }
}
