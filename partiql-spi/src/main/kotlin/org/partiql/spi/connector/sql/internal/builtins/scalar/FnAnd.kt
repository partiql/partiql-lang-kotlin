// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_AND__BOOL_BOOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "and",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", BOOL),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function and not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_AND__MISSING_BOOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "and",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", MISSING),
            FnParameter("rhs", BOOL),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function and not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_AND__BOOL_MISSING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "and",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", MISSING),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function and not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_AND__MISSING_MISSING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "and",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", MISSING),
            FnParameter("rhs", MISSING),
        ),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function and not implemented")
    }
}
