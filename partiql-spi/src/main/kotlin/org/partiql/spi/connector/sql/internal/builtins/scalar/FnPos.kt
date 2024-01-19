// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POS__INT8__INT8 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "pos",
        returns = INT8,
        parameters = listOf(FnParameter("value", INT8),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function pos not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POS__INT16__INT16 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "pos",
        returns = INT16,
        parameters = listOf(FnParameter("value", INT16),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function pos not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POS__INT32__INT32 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "pos",
        returns = INT32,
        parameters = listOf(FnParameter("value", INT32),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function pos not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POS__INT64__INT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "pos",
        returns = INT64,
        parameters = listOf(FnParameter("value", INT64),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function pos not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POS__INT__INT : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "pos",
        returns = INT,
        parameters = listOf(FnParameter("value", INT),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function pos not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "pos",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("value", DECIMAL_ARBITRARY),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function pos not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POS__FLOAT32__FLOAT32 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "pos",
        returns = FLOAT32,
        parameters = listOf(FnParameter("value", FLOAT32),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function pos not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POS__FLOAT64__FLOAT64 : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "pos",
        returns = FLOAT64,
        parameters = listOf(FnParameter("value", FLOAT64),),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function pos not implemented")
    }
}
