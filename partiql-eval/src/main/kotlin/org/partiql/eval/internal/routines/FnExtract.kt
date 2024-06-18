// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.DateValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DATE
import org.partiql.value.PType.Kind.DECIMAL_ARBITRARY
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.TIME
import org.partiql.value.PType.Kind.TIMESTAMP
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.check
import org.partiql.value.datetime.TimeZone
import org.partiql.value.decimalValue
import org.partiql.value.int32Value

//
// Extract Year
//

internal object Fn_EXTRACT_YEAR__DATE__INT : Routine {

    override val signature = FnSignature(
        name = "extract_year",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<DateValue>().value!!
        return int32Value(v.year)
    }
}


internal object Fn_EXTRACT_YEAR__TIMESTAMP__INT : Routine {

    override val signature = FnSignature(
        name = "extract_year",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.year)
    }
}

//
// Extract Month
//

internal object Fn_EXTRACT_MONTH__DATE__INT : Routine {

    override val signature = FnSignature(
        name = "extract_month",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<DateValue>().value!!
        return int32Value(v.month)
    }
}


internal object Fn_EXTRACT_MONTH__TIMESTAMP__INT : Routine {

    override val signature = FnSignature(
        name = "extract_month",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.month)
    }
}

//
//  Extract Day
//


internal object Fn_EXTRACT_DAY__DATE__INT : Routine {

    override val signature = FnSignature(
        name = "extract_day",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<DateValue>().value!!
        return int32Value(v.day)
    }
}


internal object Fn_EXTRACT_DAY__TIMESTAMP__INT : Routine {

    override val signature = FnSignature(
        name = "extract_day",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.day)
    }
}

//
// Extract Hour
//

internal object Fn_EXTRACT_HOUR__TIME__INT : Routine {

    override val signature = FnSignature(
        name = "extract_hour",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimeValue>().value!!
        return int32Value(v.hour)
    }
}


internal object Fn_EXTRACT_HOUR__TIMESTAMP__INT : Routine {

    override val signature = FnSignature(
        name = "extract_hour",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.hour)
    }
}

//
// Extract Minute
//

internal object Fn_EXTRACT_MINUTE__TIME__INT : Routine {

    override val signature = FnSignature(
        name = "extract_minute",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimeValue>().value!!
        return int32Value(v.minute)
    }
}


internal object Fn_EXTRACT_MINUTE__TIMESTAMP__INT : Routine {

    override val signature = FnSignature(
        name = "extract_minute",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.minute)
    }
}

//
// Extract Second
//

internal object Fn_EXTRACT_SECOND__TIME__DECIMAL_ARBITRARY : Routine {

    override val signature = FnSignature(
        name = "extract_second",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimeValue>().value!!
        return decimalValue(v.decimalSecond)
    }
}


internal object Fn_EXTRACT_SECOND__TIMESTAMP__DECIMAL_ARBITRARY : Routine {

    override val signature = FnSignature(
        name = "extract_second",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimestampValue>().value!!
        return decimalValue(v.decimalSecond)
    }
}

//
// Extract Timezone Hour
//

internal object Fn_EXTRACT_TIMEZONE_HOUR__TIME__INT : Routine {

    override val signature = FnSignature(
        name = "extract_timezone_hour",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimeValue>().value!!
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> int32Value(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> int32Value(tz.tzHour)
            null -> int32Value(null)
        }
    }
}


internal object Fn_EXTRACT_TIMEZONE_HOUR__TIMESTAMP__INT : Routine {

    override val signature = FnSignature(
        name = "extract_timezone_hour",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimestampValue>().value!!
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> int32Value(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> int32Value(tz.tzHour)
            null -> int32Value(null)
        }
    }
}

//
// Extract Timezone Minute
//

internal object Fn_EXTRACT_TIMEZONE_MINUTE__TIME__INT : Routine {

    override val signature = FnSignature(
        name = "extract_timezone_minute",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimeValue>().value!!
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> int32Value(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> int32Value(tz.tzMinute)
            null -> int32Value(null)
        }
    }
}


internal object Fn_EXTRACT_TIMEZONE_MINUTE__TIMESTAMP__INT : Routine {

    override val signature = FnSignature(
        name = "extract_timezone_minute",
        returns = INT,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val v = args[0].check<TimestampValue>().value!!
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> int32Value(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> int32Value(tz.tzMinute)
            null -> int32Value(null)
        }
    }
}
