// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_BETWEEN__INT8_INT8_INT8__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
            FnParameter("lower", PType.typeTinyInt()),
            FnParameter("upper", PType.typeTinyInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        @Suppress("DEPRECATION") val value = args[0].byte
        @Suppress("DEPRECATION") val lower = args[1].byte
        @Suppress("DEPRECATION") val upper = args[2].byte
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__INT16_INT16_INT16__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSmallInt()),
            FnParameter("lower", PType.typeSmallInt()),
            FnParameter("upper", PType.typeSmallInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].short
        val lower = args[1].short
        val upper = args[2].short
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__INT32_INT32_INT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeInt()),
            FnParameter("lower", PType.typeInt()),
            FnParameter("upper", PType.typeInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].int
        val lower = args[1].int
        val upper = args[2].int
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__INT64_INT64_INT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBigInt()),
            FnParameter("lower", PType.typeBigInt()),
            FnParameter("upper", PType.typeBigInt()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].long
        val lower = args[1].long
        val upper = args[2].long
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__INT_INT_INT__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeIntArbitrary()),
            @Suppress("DEPRECATION") FnParameter("lower", PType.typeIntArbitrary()),
            @Suppress("DEPRECATION") FnParameter("upper", PType.typeIntArbitrary()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bigInteger
        val lower = args[1].bigInteger
        val upper = args[2].bigInteger
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeDecimalArbitrary()),
            @Suppress("DEPRECATION") FnParameter("lower", PType.typeDecimalArbitrary()),
            @Suppress("DEPRECATION") FnParameter("upper", PType.typeDecimalArbitrary()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bigDecimal
        val lower = args[1].bigDecimal
        val upper = args[2].bigDecimal
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeReal()),
            FnParameter("lower", PType.typeReal()),
            FnParameter("upper", PType.typeReal()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].float
        val lower = args[1].float
        val upper = args[2].float
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDoublePrecision()),
            FnParameter("lower", PType.typeDoublePrecision()),
            FnParameter("upper", PType.typeDoublePrecision()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].double
        val lower = args[1].double
        val upper = args[2].double
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__STRING_STRING_STRING__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeString()),
            FnParameter("lower", PType.typeString()),
            FnParameter("upper", PType.typeString()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val lower = args[1].string
        val upper = args[2].string
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__SYMBOL_SYMBOL_SYMBOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSymbol()),
            FnParameter("lower", PType.typeSymbol()),
            FnParameter("upper", PType.typeSymbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val lower = args[1].string
        val upper = args[2].string
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeClob(Int.MAX_VALUE)),
            FnParameter("lower", PType.typeClob(Int.MAX_VALUE)),
            FnParameter("upper", PType.typeClob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bytes.toString(Charsets.UTF_8)
        val lower = args[1].bytes.toString(Charsets.UTF_8)
        val upper = args[2].bytes.toString(Charsets.UTF_8)
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__DATE_DATE_DATE__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDate()),
            FnParameter("lower", PType.typeDate()),
            FnParameter("upper", PType.typeDate()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].date
        val lower = args[1].date
        val upper = args[2].date
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__TIME_TIME_TIME__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTimeWithoutTZ(6)),
            FnParameter("lower", PType.typeTimeWithoutTZ(6)),
            FnParameter("upper", PType.typeTimeWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].time
        val lower = args[1].time
        val upper = args[2].time
        return Datum.bool(value in lower..upper)
    }
}

internal object Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTimestampWithoutTZ(6)),
            FnParameter("lower", PType.typeTimestampWithoutTZ(6)),
            FnParameter("upper", PType.typeTimestampWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].timestamp
        val lower = args[1].timestamp
        val upper = args[2].timestamp
        return Datum.bool(value in lower..upper)
    }
}
