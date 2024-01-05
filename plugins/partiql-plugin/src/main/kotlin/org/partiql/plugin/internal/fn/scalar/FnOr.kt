// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_OR__BOOL_BOOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BOOL),
            FunctionParameter("rhs", BOOL),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): BoolValue {
        val lhs = args[0].check<BoolValue>().value
        val rhs = args[1].check<BoolValue>().value
        val toReturn = when {
            lhs == true || rhs == true -> true
            lhs == null || rhs == null -> null
            else -> false
        }
        return boolValue(toReturn)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_OR__MISSING_BOOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", MISSING),
            FunctionParameter("rhs", BOOL),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val rhs = args[1].check<BoolValue>().value
        return when (rhs) {
            true -> boolValue(true)
            else -> boolValue(null)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_OR__BOOL_MISSING__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BOOL),
            FunctionParameter("rhs", MISSING),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BoolValue>().value
        return when (lhs) {
            true -> boolValue(true)
            else -> boolValue(null)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_OR__MISSING_MISSING__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", MISSING),
            FunctionParameter("rhs", MISSING),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(null)
    }
}
