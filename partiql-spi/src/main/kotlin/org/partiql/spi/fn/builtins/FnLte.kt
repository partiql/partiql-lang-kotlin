// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_LTE__INT8_INT8__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeTinyInt()),
            FnParameter("rhs", PType.typeTinyInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].byte
        val rhs = args[1].byte
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__INT16_INT16__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeSmallInt()),
            FnParameter("rhs", PType.typeSmallInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].short
        val rhs = args[1].short
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__INT32_INT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeInt()),
            FnParameter("rhs", PType.typeInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].int
        val rhs = args[1].int
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__INT64_INT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeBigInt()),
            FnParameter("rhs", PType.typeBigInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].long
        val rhs = args[1].long
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__INT_INT__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.typeIntArbitrary()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.typeIntArbitrary()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].bigInteger
        val rhs = args[1].bigInteger
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.typeDecimalArbitrary()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.typeDecimalArbitrary()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].bigDecimal
        val rhs = args[1].bigDecimal
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__FLOAT32_FLOAT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeReal()),
            FnParameter("rhs", PType.typeReal()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].float
        val rhs = args[1].float
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__FLOAT64_FLOAT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeDoublePrecision()),
            FnParameter("rhs", PType.typeDoublePrecision()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].double
        val rhs = args[1].double
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__STRING_STRING__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeString()),
            FnParameter("rhs", PType.typeString()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].string
        val rhs = args[1].string
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__SYMBOL_SYMBOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeSymbol()),
            FnParameter("rhs", PType.typeSymbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].string
        val rhs = args[1].string
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__DATE_DATE__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeDate()),
            FnParameter("rhs", PType.typeDate()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].date
        val rhs = args[1].date
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__TIME_TIME__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeTimeWithoutTZ(6)),
            FnParameter("rhs", PType.typeTimeWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].time
        val rhs = args[1].time
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__TIMESTAMP_TIMESTAMP__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeTimestampWithoutTZ(6)),
            FnParameter("rhs", PType.typeTimestampWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].timestamp
        val rhs = args[1].timestamp
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__BOOL_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("lhs", PType.typeBool()),
            FnParameter("rhs", PType.typeBool()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].boolean
        val rhs = args[1].boolean
        return Datum.bool(lhs <= rhs)
    }
}
