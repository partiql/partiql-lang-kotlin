// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_MODULO__INT8_INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "modulo",
        returns = PType.typeTinyInt(),
        parameters = listOf(
            FnParameter("lhs", PType.typeTinyInt()),
            FnParameter("rhs", PType.typeTinyInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        @Suppress("DEPRECATION") val arg0 = args[0].byte
        @Suppress("DEPRECATION") val arg1 = args[1].byte
        return Datum.tinyint((arg0 % arg1).toByte())
    }
}

internal object Fn_MODULO__INT16_INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "modulo",
        returns = PType.smallint(),
        parameters = listOf(
            FnParameter("lhs", PType.smallint()),
            FnParameter("rhs", PType.smallint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].short
        val arg1 = args[1].short
        return Datum.smallInt((arg0 % arg1).toShort())
    }
}

internal object Fn_MODULO__INT32_INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "modulo",
        returns = PType.integer(),
        parameters = listOf(
            FnParameter("lhs", PType.integer()),
            FnParameter("rhs", PType.integer()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].int
        val arg1 = args[1].int
        return Datum.integer(arg0 % arg1)
    }
}

internal object Fn_MODULO__INT64_INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "modulo",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("lhs", PType.bigint()),
            FnParameter("rhs", PType.bigint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].long
        val arg1 = args[1].long
        return Datum.bigint(arg0 % arg1)
    }
}

internal object Fn_MODULO__INT_INT__INT : Fn {

    override val signature = FnSignature(
        name = "modulo",
        returns = PType.numeric(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.numeric()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.numeric()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].bigInteger
        val arg1 = args[1].bigInteger
        return Datum.numeric(arg0 % arg1)
    }
}

internal object Fn_MODULO__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "modulo",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.decimal()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.decimal()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].bigDecimal
        val arg1 = args[1].bigDecimal
        return Datum.decimal(arg0 % arg1)
    }
}

internal object Fn_MODULO__FLOAT32_FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "modulo",
        returns = PType.real(),
        parameters = listOf(
            FnParameter("lhs", PType.real()),
            FnParameter("rhs", PType.real()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].float
        val arg1 = args[1].float
        return Datum.real(arg0 % arg1)
    }
}

internal object Fn_MODULO__FLOAT64_FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "modulo",
        returns = PType.doublePrecision(),
        parameters = listOf(
            FnParameter("lhs", PType.doublePrecision()),
            FnParameter("rhs", PType.doublePrecision()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].double
        val arg1 = args[1].double
        return Datum.doublePrecision(arg0 % arg1)
    }
}
