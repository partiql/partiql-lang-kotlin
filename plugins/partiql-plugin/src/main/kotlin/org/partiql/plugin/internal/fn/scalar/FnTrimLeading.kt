// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.plugin.internal.extensions.codepointTrimLeading
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
import org.partiql.value.stringValue

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM_LEADING__STRING__STRING : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_leading",
        returns = STRING,
        parameters = listOf(FunctionParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<StringValue>().string
        if (value == null) {
            return stringValue(null)
        }
        val result = value.codepointTrimLeading()
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM_LEADING__SYMBOL__SYMBOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_leading",
        returns = SYMBOL,
        parameters = listOf(FunctionParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<SymbolValue>().string
        if (value == null) {
            return stringValue(null)
        }
        val result = value.codepointTrimLeading()
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM_LEADING__CLOB__CLOB : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_leading",
        returns = CLOB,
        parameters = listOf(FunctionParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<ClobValue>().string
        if (value == null) {
            return stringValue(null)
        }
        val result = value.codepointTrimLeading()
        return stringValue(result)
    }
}
