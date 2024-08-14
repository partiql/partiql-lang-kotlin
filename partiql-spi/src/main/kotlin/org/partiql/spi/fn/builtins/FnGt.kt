// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_GT__INT8_INT8__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.tinyint()),
            FnParameter("rhs", PType.tinyint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.byte > rhs.byte)
    }
}

internal object Fn_GT__INT16_INT16__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.smallint()),
            FnParameter("rhs", PType.smallint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.short > rhs.short)
    }
}

internal object Fn_GT__INT32_INT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
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
        return Datum.bool(lhs > rhs)
    }
}

internal object Fn_GT__INT64_INT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.bigint()),
            FnParameter("rhs", PType.bigint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.long > rhs.long)
    }
}

internal object Fn_GT__INT_INT__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.numeric()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.numeric()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.bigInteger > rhs.bigInteger)
    }
}

internal object Fn_GT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.decimal()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.decimal()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.bigDecimal > rhs.bigDecimal)
    }
}

internal object Fn_GT__FLOAT32_FLOAT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.real()),
            FnParameter("rhs", PType.real()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.float > rhs.float)
    }
}

internal object Fn_GT__FLOAT64_FLOAT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.doublePrecision()),
            FnParameter("rhs", PType.doublePrecision()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.double > rhs.double)
    }
}

internal object Fn_GT__STRING_STRING__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.string()),
            FnParameter("rhs", PType.string()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.string > rhs.string)
    }
}

internal object Fn_GT__DATE_DATE__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.date()),
            FnParameter("rhs", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.date > rhs.date)
    }
}

internal object Fn_GT__TIME_TIME__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.time(6)),
            FnParameter("rhs", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.time > rhs.time)
    }
}

internal object Fn_GT__TIMESTAMP_TIMESTAMP__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.timestamp(6)),
            FnParameter("rhs", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.timestamp > rhs.timestamp)
    }
}

internal object Fn_GT__BOOL_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.bool()),
            FnParameter("rhs", PType.bool()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0]
        val rhs = args[1]
        return Datum.bool(lhs.boolean > rhs.boolean)
    }
}
