// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_AND__BOOL_BOOL__BOOL : Function {

    override val signature = FnSignature(
        name = "and",
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
            !lhsIsUnknown && lhs.boolean && rhsIsUnknown -> Datum.nullValue(PType.bool())
            !rhsIsUnknown && rhs.boolean && lhsIsUnknown -> Datum.nullValue(PType.bool())
            !lhs.boolean || !rhs.boolean -> Datum.bool(false)
            else -> Datum.bool(true)
        }
    }
}
