package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLikeEscape0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like_escape",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRING),
            FunctionParameter("pattern", STRING),
            FunctionParameter("escape", STRING)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like_escape not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLikeEscape1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like_escape",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SYMBOL),
            FunctionParameter("pattern", SYMBOL),
            FunctionParameter("escape", SYMBOL)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like_escape not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnLikeEscape2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like_escape",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CLOB),
            FunctionParameter("pattern", CLOB),
            FunctionParameter("escape", CLOB)
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like_escape not implemented")
    }
}
