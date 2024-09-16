// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.types.PType
import org.partiql.value.datetime.TimeZone

//
// Extract Year
//
internal object Fn_EXTRACT_YEAR__DATE__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_year",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].date
        return Datum.integer(v.year)
    }
}

internal object Fn_EXTRACT_YEAR__TIMESTAMP__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_year",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].timestamp
        return Datum.integer(v.year)
    }
}

//
// Extract Month
//
internal object Fn_EXTRACT_MONTH__DATE__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_month",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].date
        return Datum.integer(v.month)
    }
}

internal object Fn_EXTRACT_MONTH__TIMESTAMP__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_month",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].timestamp
        return Datum.integer(v.month)
    }
}

//
//  Extract Day
//

internal object Fn_EXTRACT_DAY__DATE__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_day",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].date
        return Datum.integer(v.day)
    }
}

internal object Fn_EXTRACT_DAY__TIMESTAMP__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_day",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].timestamp
        return Datum.integer(v.day)
    }
}

//
// Extract Hour
//
internal object Fn_EXTRACT_HOUR__TIME__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_hour",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].time
        return Datum.integer(v.hour)
    }
}

internal object Fn_EXTRACT_HOUR__TIMESTAMP__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_hour",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].timestamp
        return Datum.integer(v.hour)
    }
}

//
// Extract Minute
//
internal object Fn_EXTRACT_MINUTE__TIME__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_minute",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].time
        return Datum.integer(v.minute)
    }
}

internal object Fn_EXTRACT_MINUTE__TIMESTAMP__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_minute",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].timestamp
        return Datum.integer(v.minute)
    }
}

//
// Extract Second
//
internal object Fn_EXTRACT_SECOND__TIME__DECIMAL_ARBITRARY : Function {

    override val signature = FnSignature(
        name = "extract_second",
        returns = PType.decimal(),
        parameters = listOf(
            Parameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].time
        return Datum.decimal(v.decimalSecond)
    }
}

internal object Fn_EXTRACT_SECOND__TIMESTAMP__DECIMAL_ARBITRARY : Function {

    override val signature = FnSignature(
        name = "extract_second",
        returns = PType.decimal(),
        parameters = listOf(
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].timestamp
        return Datum.decimal(v.decimalSecond)
    }
}

//
// Extract Timezone Hour
//
internal object Fn_EXTRACT_TIMEZONE_HOUR__TIME__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_timezone_hour",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].time
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> Datum.integer(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> Datum.integer(tz.tzHour)
            null -> Datum.nullValue(PType.integer())
        }
    }
}

internal object Fn_EXTRACT_TIMEZONE_HOUR__TIMESTAMP__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_timezone_hour",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].timestamp
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> Datum.integer(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> Datum.integer(tz.tzHour)
            null -> Datum.nullValue(PType.integer())
        }
    }
}

//
// Extract Timezone Minute
//
internal object Fn_EXTRACT_TIMEZONE_MINUTE__TIME__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_timezone_minute",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].time
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> Datum.integer(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> Datum.integer(tz.tzMinute)
            null -> Datum.nullValue(PType.integer())
        }
    }
}

internal object Fn_EXTRACT_TIMEZONE_MINUTE__TIMESTAMP__INT32 : Function {

    override val signature = FnSignature(
        name = "extract_timezone_minute",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].timestamp
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> Datum.integer(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> Datum.integer(tz.tzMinute)
            null -> Datum.nullValue(PType.integer())
        }
    }
}
