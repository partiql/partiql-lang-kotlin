// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OR__BOOL_BOOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", BOOL),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
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

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OR__MISSING_BOOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", MISSING),
            FnParameter("rhs", BOOL),
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

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OR__BOOL_MISSING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", MISSING),
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

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OR__MISSING_MISSING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", MISSING),
            FnParameter("rhs", MISSING),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(null)
    }
}
