// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
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

@OptIn(PartiQLValueExperimental::class)
internal object Fn_POS__INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = INT8,
        parameters = listOf(FnParameter("value", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_POS__INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = INT16,
        parameters = listOf(FnParameter("value", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_POS__INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = INT32,
        parameters = listOf(FnParameter("value", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_POS__INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = INT64,
        parameters = listOf(FnParameter("value", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_POS__INT__INT : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = INT,
        parameters = listOf(FnParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("value", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_POS__FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = FLOAT32,
        parameters = listOf(FnParameter("value", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_POS__FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = FLOAT64,
        parameters = listOf(FnParameter("value", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}
