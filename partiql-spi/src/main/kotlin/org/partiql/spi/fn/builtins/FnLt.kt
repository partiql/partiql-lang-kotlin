// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_LT__INT8_INT8__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
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
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__INT16_INT16__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.smallint()),
            FnParameter("rhs", PType.smallint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].short
        val rhs = args[1].short
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__INT32_INT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.integer()),
            FnParameter("rhs", PType.integer()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].int
        val rhs = args[1].int
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__INT64_INT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.bigint()),
            FnParameter("rhs", PType.bigint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].long
        val rhs = args[1].long
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__INT_INT__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.numeric()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.numeric()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].bigInteger
        val rhs = args[1].bigInteger
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.decimal()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.decimal()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].bigDecimal
        val rhs = args[1].bigDecimal
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__FLOAT32_FLOAT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.real()),
            FnParameter("rhs", PType.real()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].float
        val rhs = args[1].float
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__FLOAT64_FLOAT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.doublePrecision()),
            FnParameter("rhs", PType.doublePrecision()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].double
        val rhs = args[1].double
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__STRING_STRING__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.string()),
            FnParameter("rhs", PType.string()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].string
        val rhs = args[1].string
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__SYMBOL_SYMBOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.symbol()),
            FnParameter("rhs", PType.symbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].string
        val rhs = args[1].string
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__DATE_DATE__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.date()),
            FnParameter("rhs", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].date
        val rhs = args[1].date
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__TIME_TIME__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.time(6)),
            FnParameter("rhs", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].time
        val rhs = args[1].time
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__TIMESTAMP_TIMESTAMP__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.timestamp(6)),
            FnParameter("rhs", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].timestamp
        val rhs = args[1].timestamp
        return Datum.bool(lhs < rhs)
    }
}

internal object Fn_LT__BOOL_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "lt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.bool()),
            FnParameter("rhs", PType.bool()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].boolean
        val rhs = args[1].boolean
        return Datum.bool(lhs < rhs)
    }
}
