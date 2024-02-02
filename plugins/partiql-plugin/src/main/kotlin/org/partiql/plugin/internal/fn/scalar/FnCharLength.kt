// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.ClobValue
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.int32Value

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_CHAR_LENGTH__STRING__INT : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "char_length",
        returns = INT32,
        parameters = listOf(
            FunctionParameter("value", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<StringValue>().value!!
        return int32Value(value.codePointCount(0, value.length))
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_CHAR_LENGTH__SYMBOL__INT : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "char_length",
        returns = INT32,
        parameters = listOf(
            FunctionParameter("lhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<SymbolValue>().value!!
        return int32Value(value.codePointCount(0, value.length))
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_CHAR_LENGTH__CLOB__INT : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "char_length",
        returns = INT32,
        parameters = listOf(
            FunctionParameter("lhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<ClobValue>().value!!
        return int32Value(value.size)
    }
}
