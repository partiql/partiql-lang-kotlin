// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_AND__BOOL_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "and",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeBool()),
            FnParameter("rhs", PType.typeBool()),
        ),
        isNullable = true,
        isNullCall = false,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        // SQL:1999 Section 6.30 Table 13
        return when {
            lhs.isNull && rhs.isNull -> Datum.nullValue(PType.typeBool())
            lhs.boolean && rhs.isNull -> Datum.nullValue(PType.typeBool())
            rhs.boolean && lhs.isNull -> Datum.nullValue(PType.typeBool())
            !lhs.boolean || !rhs.boolean -> Datum.bool(false)
            else -> Datum.bool(true)
        }
    }
}
