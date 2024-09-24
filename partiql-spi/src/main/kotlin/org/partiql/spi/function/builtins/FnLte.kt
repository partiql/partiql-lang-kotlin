// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_LTE__INT8_INT8__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.tinyint()),
            Parameter("rhs", PType.tinyint()),
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

internal object Fn_LTE__INT16_INT16__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.smallint()),
            Parameter("rhs", PType.smallint()),
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

internal object Fn_LTE__INT32_INT32__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
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
        return Datum.bool(lhs <= rhs)
    }
}

internal object Fn_LTE__INT64_INT64__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.bigint()),
            Parameter("rhs", PType.bigint()),
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

internal object Fn_LTE__INT_INT__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("lhs", PType.numeric()),
            @Suppress("DEPRECATION") Parameter("rhs", PType.numeric()),
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

internal object Fn_LTE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("lhs", PType.decimal()),
            @Suppress("DEPRECATION") Parameter("rhs", PType.decimal()),
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

internal object Fn_LTE__FLOAT32_FLOAT32__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.real()),
            Parameter("rhs", PType.real()),
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

internal object Fn_LTE__FLOAT64_FLOAT64__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.doublePrecision()),
            Parameter("rhs", PType.doublePrecision()),
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

internal object Fn_LTE__STRING_STRING__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.string()),
            Parameter("rhs", PType.string()),
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

internal object Fn_LTE__SYMBOL_SYMBOL__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.symbol()),
            Parameter("rhs", PType.symbol()),
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

internal object Fn_LTE__DATE_DATE__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.date()),
            Parameter("rhs", PType.date()),
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

internal object Fn_LTE__TIME_TIME__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.time(6)),
            Parameter("rhs", PType.time(6)),
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

internal object Fn_LTE__TIMESTAMP_TIMESTAMP__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.timestamp(6)),
            Parameter("rhs", PType.timestamp(6)),
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

internal object Fn_LTE__BOOL_BOOL__BOOL : Function {

    override val signature = FnSignature(
        name = "lte",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("lhs", PType.bool()),
            Parameter("rhs", PType.bool()),
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
