// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

// TODO: Handle Overflow
internal object Fn_PLUS__INT8_INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "plus",
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
        return Datum.tinyInt((arg0 + arg1).toByte())
    }
}

internal object Fn_PLUS__INT16_INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.typeSmallInt(),
        parameters = listOf(
            FnParameter("lhs", PType.typeSmallInt()),
            FnParameter("rhs", PType.typeSmallInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].short
        val arg1 = args[1].short
        return Datum.smallInt((arg0 + arg1).toShort())
    }
}

internal object Fn_PLUS__INT32_INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("lhs", PType.typeInt()),
            FnParameter("rhs", PType.typeInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].int
        val arg1 = args[1].int
        return Datum.integer(arg0 + arg1)
    }
}

internal object Fn_PLUS__INT64_INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.typeBigInt(),
        parameters = listOf(
            FnParameter("lhs", PType.typeBigInt()),
            FnParameter("rhs", PType.typeBigInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].long
        val arg1 = args[1].long
        return Datum.bigInt(arg0 + arg1)
    }
}

internal object Fn_PLUS__INT_INT__INT : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.typeIntArbitrary(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.typeIntArbitrary()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.typeIntArbitrary()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].bigInteger
        val arg1 = args[1].bigInteger
        return Datum.intArbitrary(arg0 + arg1)
    }
}

internal object Fn_PLUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.typeDecimalArbitrary()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.typeDecimalArbitrary()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].bigDecimal
        val arg1 = args[1].bigDecimal
        return Datum.decimalArbitrary(arg0 + arg1)
    }
}

internal object Fn_PLUS__FLOAT32_FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.typeReal(),
        parameters = listOf(
            FnParameter("lhs", PType.typeReal()),
            FnParameter("rhs", PType.typeReal()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].float
        val arg1 = args[1].float
        return Datum.real(arg0 + arg1)
    }
}

internal object Fn_PLUS__FLOAT64_FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.typeDoublePrecision(),
        parameters = listOf(
            FnParameter("lhs", PType.typeDoublePrecision()),
            FnParameter("rhs", PType.typeDoublePrecision()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].double
        val arg1 = args[1].double
        return Datum.doublePrecision(arg0 + arg1)
    }
}
