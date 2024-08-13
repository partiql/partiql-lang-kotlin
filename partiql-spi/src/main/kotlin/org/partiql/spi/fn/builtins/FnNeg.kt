// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

// TODO: Handle Overflow
internal object Fn_NEG__INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.typeTinyInt(),
        parameters = listOf(FnParameter("value", PType.typeTinyInt())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        @Suppress("DEPRECATION")
        val value = args[0].byte
        return Datum.tinyint(value.times(-1).toByte())
    }
}

internal object Fn_NEG__INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.smallint(),
        parameters = listOf(FnParameter("value", PType.smallint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].short
        return Datum.smallInt(value.times(-1).toShort())
    }
}

internal object Fn_NEG__INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.integer(),
        parameters = listOf(FnParameter("value", PType.integer())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].int
        return Datum.integer(value.times(-1))
    }
}

internal object Fn_NEG__INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.bigint(),
        parameters = listOf(FnParameter("value", PType.bigint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].long
        return Datum.bigint(value.times(-1L))
    }
}

internal object Fn_NEG__INT__INT : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.numeric(),
        parameters = listOf(FnParameter("value", PType.numeric())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bigInteger
        return Datum.numeric(value.negate())
    }
}

internal object Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.decimal(),
        parameters = listOf(FnParameter("value", PType.decimal())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bigDecimal
        return Datum.decimal(value.negate())
    }
}

internal object Fn_NEG__FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.real(),
        parameters = listOf(FnParameter("value", PType.real())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].float
        return Datum.real(value.times(-1))
    }
}

internal object Fn_NEG__FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.doublePrecision(),
        parameters = listOf(FnParameter("value", PType.doublePrecision())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].double
        return Datum.doublePrecision(value.times(-1))
    }
}
