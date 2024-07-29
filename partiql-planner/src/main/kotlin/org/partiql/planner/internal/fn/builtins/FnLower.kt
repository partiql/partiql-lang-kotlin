// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.check
import org.partiql.value.stringValue

@OptIn(PartiQLValueExperimental::class)
internal object Fn_LOWER__STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "lower",
        returns = STRING,
        parameters = listOf(FnParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<StringValue>().string!!
        val result = string.lowercase()
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_LOWER__SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "lower",
        returns = SYMBOL,
        parameters = listOf(FnParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<StringValue>().string!!
        val result = string.lowercase()
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_LOWER__CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "lower",
        returns = CLOB,
        parameters = listOf(FnParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<StringValue>().string!!
        val result = string.lowercase()
        return stringValue(result)
    }
}
