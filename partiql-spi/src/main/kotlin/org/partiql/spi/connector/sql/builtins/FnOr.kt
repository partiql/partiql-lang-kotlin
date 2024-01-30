// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OR__BOOL_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", BOOL),
        ),
        isNullCall = false,
        isNullable = true,
        isMissingCall = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function or not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OR__MISSING_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", MISSING),
            FnParameter("rhs", BOOL),
        ),
        isNullCall = false,
        isNullable = true,
        isMissingCall = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function or not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OR__BOOL_MISSING__BOOL : Fn {

    override val signature = FnSignature(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", MISSING),
        ),
        isNullCall = false,
        isNullable = true,
        isMissingCall = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function or not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OR__MISSING_MISSING__BOOL : Fn {

    override val signature = FnSignature(
        name = "or",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", MISSING),
            FnParameter("rhs", MISSING),
        ),
        isNullCall = false,
        isNullable = true,
        isMissingCall = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function or not implemented")
    }
}
