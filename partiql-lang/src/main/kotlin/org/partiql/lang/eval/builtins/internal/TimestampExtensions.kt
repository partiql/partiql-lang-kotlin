package org.partiql.lang.eval.builtins.internal

import com.amazon.ion.Timestamp
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.time.SECONDS_PER_MINUTE
import org.partiql.lang.eval.time.Time
import org.partiql.lang.syntax.DateTimePart
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal val precisionOrder = listOf(
    Timestamp.Precision.YEAR,
    Timestamp.Precision.MONTH,
    Timestamp.Precision.DAY,
    Timestamp.Precision.MINUTE,
    Timestamp.Precision.SECOND
)

internal val dateTimePartToPrecision = mapOf(
    DateTimePart.YEAR to Timestamp.Precision.YEAR,
    DateTimePart.MONTH to Timestamp.Precision.MONTH,
    DateTimePart.DAY to Timestamp.Precision.DAY,
    DateTimePart.HOUR to Timestamp.Precision.MINUTE,
    DateTimePart.MINUTE to Timestamp.Precision.MINUTE,
    DateTimePart.SECOND to Timestamp.Precision.SECOND
)

internal fun Timestamp.hasSufficientPrecisionFor(requiredPrecision: Timestamp.Precision): Boolean {
    val requiredPrecisionPos = precisionOrder.indexOf(requiredPrecision)
    val precisionPos = precisionOrder.indexOf(precision)

    return precisionPos >= requiredPrecisionPos
}

internal fun Timestamp.adjustPrecisionTo(dateTimePart: DateTimePart): Timestamp {
    val requiredPrecision = dateTimePartToPrecision[dateTimePart]!!

    if (this.hasSufficientPrecisionFor(requiredPrecision)) {
        return this
    }

    return when (requiredPrecision) {
        Timestamp.Precision.YEAR -> Timestamp.forYear(this.year)
        Timestamp.Precision.MONTH -> Timestamp.forMonth(this.year, this.month)
        Timestamp.Precision.DAY -> Timestamp.forDay(this.year, this.month, this.day)
        Timestamp.Precision.SECOND -> Timestamp.forSecond(
            this.year, this.month, this.day, this.hour, this.minute, this.second, this.localOffset
        )
        Timestamp.Precision.MINUTE -> Timestamp.forMinute(
            this.year, this.month, this.day, this.hour, this.minute, this.localOffset
        )
        else -> errNoContext(
            "invalid datetime part for date_add: ${dateTimePart.toString().toLowerCase()}",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_DATE_PART,
            internal = false
        )
    }
}

internal fun Timestamp.toOffsetDateTime() = OffsetDateTime.of(
    year, month, day, hour, minute, second, 0, ZoneOffset.ofTotalSeconds((localOffset ?: 0) * 60)
)

// IonJava Timestamp.localOffset is the offset in minutes, e.g.: `+01:00 = 60` and `-1:20 = -80`
internal fun Timestamp.hourOffset() = (localOffset ?: 0) / SECONDS_PER_MINUTE

internal fun Timestamp.minuteOffset() = (localOffset ?: 0) % SECONDS_PER_MINUTE

internal fun Timestamp.extractedValue(dateTimePart: DateTimePart): BigDecimal {
    return when (dateTimePart) {
        DateTimePart.YEAR -> year
        DateTimePart.MONTH -> month
        DateTimePart.DAY -> day
        DateTimePart.HOUR -> hour
        DateTimePart.MINUTE -> minute
        DateTimePart.SECOND -> second
        DateTimePart.TIMEZONE_HOUR -> hourOffset()
        DateTimePart.TIMEZONE_MINUTE -> minuteOffset()
    }.toBigDecimal()
}

internal fun LocalDate.extractedValue(dateTimePart: DateTimePart): BigDecimal {
    return when (dateTimePart) {
        DateTimePart.YEAR -> year
        DateTimePart.MONTH -> monthValue
        DateTimePart.DAY -> dayOfMonth
        DateTimePart.TIMEZONE_HOUR,
        DateTimePart.TIMEZONE_MINUTE -> errNoContext(
            "Timestamp unit ${dateTimePart.name.toLowerCase()} not supported for DATE type",
            ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            internal = false
        )
        DateTimePart.HOUR, DateTimePart.MINUTE, DateTimePart.SECOND -> 0
    }.toBigDecimal()
}

internal fun Time.extractedValue(dateTimePart: DateTimePart): BigDecimal {
    return when (dateTimePart) {
        DateTimePart.HOUR -> localTime.hour.toBigDecimal()
        DateTimePart.MINUTE -> localTime.minute.toBigDecimal()
        DateTimePart.SECOND -> secondsWithFractionalPart
        DateTimePart.TIMEZONE_HOUR -> timezoneHour?.toBigDecimal() ?: errNoContext(
            "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type without TIME ZONE",
            ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            internal = false
        )
        DateTimePart.TIMEZONE_MINUTE -> timezoneMinute?.toBigDecimal() ?: errNoContext(
            "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type without TIME ZONE",
            ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            internal = false
        )
        DateTimePart.YEAR, DateTimePart.MONTH, DateTimePart.DAY -> errNoContext(
            "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type.",
            ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            internal = false
        )
    }
}
