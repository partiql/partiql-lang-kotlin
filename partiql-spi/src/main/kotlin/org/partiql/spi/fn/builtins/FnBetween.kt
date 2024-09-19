// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_BETWEEN__INT8_INT8_INT8__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.tinyint()),
            Parameter("lower", PType.tinyint()),
            Parameter("upper", PType.tinyint()),
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

internal object Fn_BETWEEN__INT16_INT16_INT16__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.smallint()),
            Parameter("lower", PType.smallint()),
            Parameter("upper", PType.smallint()),
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

internal object Fn_BETWEEN__INT32_INT32_INT32__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.integer()),
            Parameter("lower", PType.integer()),
            Parameter("upper", PType.integer()),
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

internal object Fn_BETWEEN__INT64_INT64_INT64__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bigint()),
            Parameter("lower", PType.bigint()),
            Parameter("upper", PType.bigint()),
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

internal object Fn_BETWEEN__INT_INT_INT__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
            @Suppress("DEPRECATION") Parameter("lower", PType.numeric()),
            @Suppress("DEPRECATION") Parameter("upper", PType.numeric()),
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

internal object Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL :
    Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
            @Suppress("DEPRECATION") Parameter("lower", PType.decimal()),
            @Suppress("DEPRECATION") Parameter("upper", PType.decimal()),
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

internal object Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.real()),
            Parameter("lower", PType.real()),
            Parameter("upper", PType.real()),
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

internal object Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.doublePrecision()),
            Parameter("lower", PType.doublePrecision()),
            Parameter("upper", PType.doublePrecision()),
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

internal object Fn_BETWEEN__STRING_STRING_STRING__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.string()),
            Parameter("lower", PType.string()),
            Parameter("upper", PType.string()),
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

internal object Fn_BETWEEN__SYMBOL_SYMBOL_SYMBOL__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.symbol()),
            Parameter("lower", PType.symbol()),
            Parameter("upper", PType.symbol()),
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

internal object Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.clob(Int.MAX_VALUE)),
            Parameter("lower", PType.clob(Int.MAX_VALUE)),
            Parameter("upper", PType.clob(Int.MAX_VALUE)),
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

internal object Fn_BETWEEN__DATE_DATE_DATE__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.date()),
            Parameter("lower", PType.date()),
            Parameter("upper", PType.date()),
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

internal object Fn_BETWEEN__TIME_TIME_TIME__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.time(6)),
            Parameter("lower", PType.time(6)),
            Parameter("upper", PType.time(6)),
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

internal object Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL : Function {

    override val signature = FnSignature(
        name = "between",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.timestamp(6)),
            Parameter("lower", PType.timestamp(6)),
            Parameter("upper", PType.timestamp(6)),
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
