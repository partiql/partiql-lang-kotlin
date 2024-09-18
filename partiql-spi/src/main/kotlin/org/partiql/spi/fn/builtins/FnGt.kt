// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_GT__INT8_INT8__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.tinyint()),
            Parameter("rhs", PType.tinyint()),
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

internal object Fn_GT__INT16_INT16__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.smallint()),
            Parameter("rhs", PType.smallint()),
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

internal object Fn_GT__INT32_INT32__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.integer()),
            Parameter("rhs", PType.integer()),
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

internal object Fn_GT__INT64_INT64__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.bigint()),
            Parameter("rhs", PType.bigint()),
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

internal object Fn_GT__INT_INT__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("lhs", PType.numeric()),
            @Suppress("DEPRECATION") Parameter("rhs", PType.numeric()),
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

internal object Fn_GT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("lhs", PType.decimal()),
            @Suppress("DEPRECATION") Parameter("rhs", PType.decimal()),
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

internal object Fn_GT__FLOAT32_FLOAT32__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.real()),
            Parameter("rhs", PType.real()),
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

internal object Fn_GT__FLOAT64_FLOAT64__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.doublePrecision()),
            Parameter("rhs", PType.doublePrecision()),
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

internal object Fn_GT__STRING_STRING__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.string()),
            Parameter("rhs", PType.string()),
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

internal object Fn_GT__SYMBOL_SYMBOL__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.symbol()),
            Parameter("rhs", PType.symbol()),
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

internal object Fn_GT__DATE_DATE__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.date()),
            Parameter("rhs", PType.date()),
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

internal object Fn_GT__TIME_TIME__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.time(6)),
            Parameter("rhs", PType.time(6)),
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

internal object Fn_GT__TIMESTAMP_TIMESTAMP__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.timestamp(6)),
            Parameter("rhs", PType.timestamp(6)),
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

internal object Fn_GT__BOOL_BOOL__BOOL : Function {

    override val signature = FnSignature(
        name = "gt",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.bool()),
            Parameter("rhs", PType.bool()),
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
