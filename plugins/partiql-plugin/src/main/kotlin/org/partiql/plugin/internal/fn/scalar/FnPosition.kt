// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.plugin.internal.extensions.codepointPosition
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.ClobValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.int64Value
import org.partiql.value.stringValue

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POSITION__STRING_STRING__INT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FunctionParameter("probe", STRING),
            FunctionParameter("value", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val s1 = args[0].check<StringValue>().string
        val s2 = args[1].check<StringValue>().string
        if (s1 == null || s2 == null) {
            return stringValue(null)
        }
        val result = s2.codepointPosition(s1)
        return int64Value(result.toLong())
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POSITION__SYMBOL_SYMBOL__INT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FunctionParameter("probe", SYMBOL),
            FunctionParameter("value", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val s1 = args[0].check<SymbolValue>().string
        val s2 = args[1].check<SymbolValue>().string
        if (s1 == null || s2 == null) {
            return stringValue(null)
        }
        val result = s2.codepointPosition(s1)
        return int64Value(result.toLong())
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POSITION__CLOB_CLOB__INT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FunctionParameter("probe", CLOB),
            FunctionParameter("value", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val s1 = args[0].check<ClobValue>().string
        val s2 = args[1].check<ClobValue>().string
        if (s1 == null || s2 == null) {
            return stringValue(null)
        }
        val result = s2.codepointPosition(s1)
        return int64Value(result.toLong())
    }
}
