// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
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
internal object Fn_NEG__INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT8,
        parameters = listOf(FnParameter("value", INT8),),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT16,
        parameters = listOf(FnParameter("value", INT16),),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT32,
        parameters = listOf(FnParameter("value", INT32),),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT64,
        parameters = listOf(FnParameter("value", INT64),),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT__INT : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT,
        parameters = listOf(FnParameter("value", INT),),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("value", DECIMAL_ARBITRARY),),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = FLOAT32,
        parameters = listOf(FnParameter("value", FLOAT32),),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = FLOAT64,
        parameters = listOf(FnParameter("value", FLOAT64),),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}
