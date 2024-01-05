// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
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

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_CONCAT__STRING_STRING__STRING : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "concat",
        returns = STRING,
        parameters = listOf(
            FunctionParameter("lhs", STRING),
            FunctionParameter("rhs", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): StringValue {

        val arg0 = args[0].check<StringValue>().value!!
        val arg1 = args[1].check<StringValue>().value!!
        return stringValue(arg0 + arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_CONCAT__SYMBOL_SYMBOL__SYMBOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "concat",
        returns = SYMBOL,
        parameters = listOf(
            FunctionParameter("lhs", SYMBOL),
            FunctionParameter("rhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: We are still debating on whether symbol is a value. It looks like it may not be, and therefore, this
    //  will be removed.
    override fun invoke(args: Array<PartiQLValue>): SymbolValue {
        val arg0 = args[0].check<SymbolValue>().value!!
        val arg1 = args[1].check<SymbolValue>().value!!
        return symbolValue(arg0 + arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_CONCAT__CLOB_CLOB__CLOB : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "concat",
        returns = CLOB,
        parameters = listOf(
            FunctionParameter("lhs", CLOB),
            FunctionParameter("rhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): ClobValue {
        val arg0 = args[0].check<ClobValue>().value!!
        val arg1 = args[1].check<ClobValue>().value!!
        return clobValue(arg0 + arg1)
    }
}
