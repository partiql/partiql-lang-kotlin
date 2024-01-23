// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.clobValue
import org.partiql.value.stringValue
import org.partiql.value.symbolValue

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CONCAT__STRING_STRING__STRING : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "concat",
        returns = STRING,
        parameters = listOf(
            FnParameter("lhs", STRING),
            FnParameter("rhs", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<StringValue>().value!!
        val arg1 = args[1].check<StringValue>().value!!
        return stringValue(arg0 + arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CONCAT__SYMBOL_SYMBOL__SYMBOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "concat",
        returns = SYMBOL,
        parameters = listOf(
            FnParameter("lhs", SYMBOL),
            FnParameter("rhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<SymbolValue>().value!!
        val arg1 = args[1].check<SymbolValue>().value!!
        return symbolValue(arg0 + arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CONCAT__CLOB_CLOB__CLOB : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "concat",
        returns = CLOB,
        parameters = listOf(
            FnParameter("lhs", CLOB),
            FnParameter("rhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<ClobValue>().value!!
        val arg1 = args[1].check<ClobValue>().value!!
        return clobValue(arg0 + arg1)
    }
}
