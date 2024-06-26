// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.DateValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.check
import org.partiql.value.datetime.TimeZone
import org.partiql.value.decimalValue
import org.partiql.value.int32Value

//
// Extract Year
//
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_YEAR__DATE__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_year",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<DateValue>().value!!
        return int32Value(v.year)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_YEAR__TIMESTAMP__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_year",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.year)
    }
}

//
// Extract Month
//
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_MONTH__DATE__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_month",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<DateValue>().value!!
        return int32Value(v.month)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_MONTH__TIMESTAMP__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_month",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.month)
    }
}

//
//  Extract Day
//

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_DAY__DATE__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_day",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<DateValue>().value!!
        return int32Value(v.day)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_DAY__TIMESTAMP__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_day",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.day)
    }
}

//
// Extract Hour
//
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_HOUR__TIME__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_hour",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimeValue>().value!!
        return int32Value(v.hour)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_HOUR__TIMESTAMP__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_hour",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.hour)
    }
}

//
// Extract Minute
//
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_MINUTE__TIME__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_minute",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimeValue>().value!!
        return int32Value(v.minute)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_MINUTE__TIMESTAMP__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_minute",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimestampValue>().value!!
        return int32Value(v.minute)
    }
}

//
// Extract Second
//
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_SECOND__TIME__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "extract_second",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimeValue>().value!!
        return decimalValue(v.decimalSecond)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_SECOND__TIMESTAMP__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "extract_second",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimestampValue>().value!!
        return decimalValue(v.decimalSecond)
    }
}

//
// Extract Timezone Hour
//
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_TIMEZONE_HOUR__TIME__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_timezone_hour",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimeValue>().value!!
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> int32Value(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> int32Value(tz.tzHour)
            null -> int32Value(null)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_TIMEZONE_HOUR__TIMESTAMP__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_timezone_hour",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
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
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_TIMEZONE_MINUTE__TIME__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_timezone_minute",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimeValue>().value!!
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> int32Value(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> int32Value(tz.tzMinute)
            null -> int32Value(null)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EXTRACT_TIMEZONE_MINUTE__TIMESTAMP__INT32 : Fn {

    override val signature = FnSignature(
        name = "extract_timezone_minute",
        returns = INT32,
        parameters = listOf(
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val v = args[0].check<TimestampValue>().value!!
        return when (val tz = v.timeZone) {
            TimeZone.UnknownTimeZone -> int32Value(0) // TODO: Should this be NULL?
            is TimeZone.UtcOffset -> int32Value(tz.tzMinute)
            null -> int32Value(null)
        }
    }
}
