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
        returns = PType.smallint(),
        parameters = listOf(FnParameter("value", PType.smallint())),
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
        returns = PType.integer(),
        parameters = listOf(FnParameter("value", PType.integer())),
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
        returns = PType.bigint(),
        parameters = listOf(FnParameter("value", PType.bigint())),
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
        returns = PType.numeric(),
        parameters = listOf(FnParameter("value", PType.numeric())),
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
        returns = PType.decimal(),
        parameters = listOf(FnParameter("value", PType.decimal())),
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
        returns = PType.real(),
        parameters = listOf(FnParameter("value", PType.real())),
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
        returns = PType.doublePrecision(),
        parameters = listOf(FnParameter("value", PType.doublePrecision())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}
