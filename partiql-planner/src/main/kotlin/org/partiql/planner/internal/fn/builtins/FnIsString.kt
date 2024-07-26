// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.errors.TypeCheckException
import org.partiql.planner.internal.fn.Fn

import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.StringValue
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class)
internal object Fn_IS_STRING__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_string",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(args[0] is StringValue)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_IS_STRING__INT32_ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_string",
        returns = BOOL,
        parameters = listOf(
            FnParameter("type_parameter_1", INT32),
            FnParameter("value", ANY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[1]
        if (v !is StringValue) {
            return boolValue(false)
        }
        val length = args[0].check<Int32Value>().value
        if (length == null || length < 0) {
            throw TypeCheckException()
        }
        return boolValue(v.value!!.length <= length)
    }
}
