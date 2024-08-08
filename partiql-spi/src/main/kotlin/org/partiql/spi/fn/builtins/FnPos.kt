// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_POS__INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.typeTinyInt(),
        parameters = listOf(FnParameter("value", PType.typeTinyInt())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.typeSmallInt(),
        parameters = listOf(FnParameter("value", PType.typeSmallInt())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.typeInt(),
        parameters = listOf(FnParameter("value", PType.typeInt())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.typeBigInt(),
        parameters = listOf(FnParameter("value", PType.typeBigInt())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__INT__INT : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.typeIntArbitrary(),
        parameters = listOf(FnParameter("value", PType.typeIntArbitrary())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(FnParameter("value", PType.typeDecimalArbitrary())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.typeReal(),
        parameters = listOf(FnParameter("value", PType.typeReal())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.typeDoublePrecision(),
        parameters = listOf(FnParameter("value", PType.typeDoublePrecision())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}
