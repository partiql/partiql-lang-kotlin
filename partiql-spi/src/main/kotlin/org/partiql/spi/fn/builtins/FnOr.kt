// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.types.PType

internal object Fn_OR__BOOL_BOOL__BOOL : Function {

    override val signature = FnSignature(
        name = "or",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.bool()),
            Parameter("rhs", PType.bool()),
        ),
        isNullable = true,
        isNullCall = false,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        val lhsIsUnknown = lhs.isNull || lhs.isMissing
        val rhsIsUnknown = rhs.isNull || rhs.isMissing

        // SQL:1999 Section 6.30 Table 13
        return when {
            lhsIsUnknown && rhsIsUnknown -> Datum.nullValue(PType.bool())
            !lhsIsUnknown && !rhsIsUnknown -> Datum.bool(lhs.boolean || rhs.boolean)
            lhsIsUnknown && rhs.boolean -> Datum.bool(true)
            rhsIsUnknown && lhs.boolean -> Datum.bool(true)
            else -> Datum.nullValue(PType.bool())
        }
    }
}
