package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnSubstring0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "substring",
        returns = STRING,
        parameters = listOf(FunctionParameter("value", STRING), FunctionParameter("start", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnSubstring1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "substring",
        returns = STRING,
        parameters = listOf(
            FunctionParameter("value", STRING),
            FunctionParameter("start", INT64),
            FunctionParameter("end", INT64)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnSubstring2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "substring",
        returns = SYMBOL,
        parameters = listOf(FunctionParameter("value", SYMBOL), FunctionParameter("start", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnSubstring3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "substring",
        returns = SYMBOL,
        parameters = listOf(
            FunctionParameter("value", SYMBOL),
            FunctionParameter("start", INT64),
            FunctionParameter("end", INT64)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnSubstring4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "substring",
        returns = CLOB,
        parameters = listOf(FunctionParameter("value", CLOB), FunctionParameter("start", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnSubstring5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "substring",
        returns = CLOB,
        parameters = listOf(
            FunctionParameter("value", CLOB),
            FunctionParameter("start", INT64),
            FunctionParameter("end", INT64)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function substring not implemented")
    }
}
