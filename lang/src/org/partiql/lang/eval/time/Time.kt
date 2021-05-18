package org.partiql.lang.eval.time

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.err
import org.partiql.lang.util.getOffsetHHmm
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.times
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DateTimeException
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.math.min

// Constants related to the TIME

internal const val PARTIQL_TIME_ANNOTATION = "\$partiql_time"
internal const val HOURS_PER_DAY = 24
internal const val MINUTES_PER_HOUR = 60
internal const val SECONDS_PER_MINUTE = 60
internal const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
internal const val NANOS_PER_SECOND = 1000000000.0
internal const val MAX_PRECISION_FOR_TIME = 9


/**
 * Wrapper class representing the run time instance of TIME in PartiQL.
 * - `TIME [(p)] HH:MM:ss[.ddd][+|-HH:MM]` PartiQL statement creates a run-time instance of this class with [zoneOffset] as null.
 * - `TIME WITH TIME ZONE [(p)] HH:MM:ss[.ddd][+|-HH:MM]` PartiQL statement creates a run-time instance of this class with both
 * [localTime] and [zoneOffset] defined.
 *
 * @param localTime Represents the time component irrespective of the [ZoneOffset].
 * @param zoneOffset Represents the time zone of the TIME instance.
 *  If the [zoneOffset] is null, the [Time] instance has no defined time zone.
 * @param precision Represents the number of significant digits in fractional part of the second's value of the TIME instance.
 *  The default precision is 9 meaning, the default precision is of nanoseconds.
 *  If the precision is specified by the user, the fractional part of the second will be rounded to the specified precision.
 *  For eg, `TIME (3) 23:46:58.1267` will be stored in this instance with [localTime] as `23:46:58.127000000` along with preserving the
 *  [precision] value as 3.
 *  Note that the [LocalTime] always stores the fractional part of the second in nanoseconds.
 *  It is up to the application developers to make use of the preserved [precision] value as need be.
 */
data class Time(val localTime: LocalTime, val precision: Int = MAX_PRECISION_FOR_TIME, val zoneOffset: ZoneOffset? = null) {

    init {
        // Validate that the precision value is between 0 and 9 inclusive.
        if (precision < 0 || precision > MAX_PRECISION_FOR_TIME) {
            err(
                message = "Specified precision for TIME should be a non-negative integer between 0 and 9 inclusive",
                errorCode = ErrorCode.EVALUATOR_INVALID_PRECISION_FOR_TIME,
                errorContext = propertyValueMapOf(),
                internal = false)
        }
    }

    companion object {

        /**
         * Gives an instance of [Time] with the specified hour, minute, second, nano, precision and tz_minutes.
         * Note that the precision applies for the fractional part of the second's value.
         * However the fractional part will always be stored in the nanoseconds along with preserving the precision separately.
         * If the specified precision is less than 9 (nanosecond's precision), then the fractional part of the second i.e. nano field is
         * rounded up to the given precision.
         * For e.g., if [nano] is 126700000 and the [precision] is 3, the [nano] will be rounded to 127000000.
         * Note that the [nano] will still store the value in nanoseconds (0's padded after the desired precision digits),
         * however the [Time] instance will preserve the [precision] thereby preserving the entire original value.
         */
        @JvmOverloads
        fun of(hour: Int, minute: Int, second: Int, nano: Int, precision: Int, tz_minutes: Int? = null) : Time {

            //Validates the range of values for all the parameters. This part may throw a DateTimeException
            try {
                ChronoField.HOUR_OF_DAY.checkValidValue(hour.toLong())
                ChronoField.MINUTE_OF_HOUR.checkValidValue(minute.toLong())
                ChronoField.SECOND_OF_MINUTE.checkValidValue(second.toLong())
                ChronoField.NANO_OF_SECOND.checkValidValue(nano.toLong())
                tz_minutes?.let {
                    ChronoField.OFFSET_SECONDS.checkValidIntValue((it * SECONDS_PER_MINUTE).toLong())
                }
            } catch (dte: DateTimeException) {
                throw EvaluationException(dte, ErrorCode.EVALUATOR_DATETIME_EXCEPTION, propertyValueMapOf(), false)
            }

            // Round nanoseconds to the given precision.
            val nanoWithPrecision = when (precision) {
                MAX_PRECISION_FOR_TIME -> nano
                else -> (BigDecimal(nano / NANOS_PER_SECOND).setScale(precision, RoundingMode.HALF_UP) * NANOS_PER_SECOND).toInt()
            }
            // If the nanos are added to form up a whole second because of the specified precision, carry over the second all up to the hour
            // and use the mod values of all the new fields to fit in the valid range.
            val newNano = nanoWithPrecision % NANOS_PER_SECOND.toInt()
            val newSecond = second + (nanoWithPrecision / NANOS_PER_SECOND.toInt())
            val newMinute = minute + (newSecond / SECONDS_PER_MINUTE)
            val newHour = (hour + (newMinute / MINUTES_PER_HOUR))
            // Since all the values are checked for range, this call will not throw a DateTimeException.
            val localTime = LocalTime.of(
                newHour % HOURS_PER_DAY,
                newMinute % MINUTES_PER_HOUR,
                newSecond % SECONDS_PER_MINUTE,
                newNano
            )
            val zoneOffset = tz_minutes?.let {
                ZoneOffset.ofTotalSeconds( it * SECONDS_PER_MINUTE)
            }
            return Time(localTime, precision, zoneOffset)
        }
    }

    /**
     * Returns the [OffsetTime] representation of this value if a [ZoneOffset] is defined for this, otherwise returns null.
     */
    val offsetTime
        get() : OffsetTime? = zoneOffset?.let {
            OffsetTime.of(localTime, it)
        }

    /**
     * Returns the TIMEZONE_HOUR for the [zoneOffset] of this instance.
     */
    val timezoneHour
        get() : Int? = zoneOffset?.totalSeconds?.div(SECONDS_PER_HOUR)

    /**
     * Returns the TIMEZONE_HOUR for the [zoneOffset] of this instance.
     */
    val timezoneMinute
        get() : Int? = (zoneOffset?.totalSeconds?.div(SECONDS_PER_MINUTE))?.rem(SECONDS_PER_MINUTE)

    /**
     * Returns the seconds along with the fractional part of the second's value.
     */
    val secondsWithFractionalPart
        get()  = localTime.second.toBigDecimal() + localTime.nano.toBigDecimal() / NANOS_PER_SECOND.toBigDecimal()

    fun toIonValue(ion: IonSystem): IonStruct =
        ion.newEmptyStruct().apply {
            add("hour", ion.newInt(localTime.hour))
            add("minute", ion.newInt(localTime.minute))
            add("second", ion.newDecimal(BigDecimal(localTime.second + localTime.nano / NANOS_PER_SECOND).setScale(
                precision, RoundingMode.HALF_UP)))
            add("timezone_hour", ion.newInt(zoneOffset?.totalSeconds?.div(SECONDS_PER_HOUR)))
            add("timezone_minute", ion.newInt(zoneOffset?.totalSeconds?.div((SECONDS_PER_MINUTE))?.rem(60)))
            addTypeAnnotation(PARTIQL_TIME_ANNOTATION)
        }

    /**
    + Generates a formatter pattern at run time depending on the precision value.
    * This pattern is subject to change based on the java's [DateTimeFormatter]. [java doc](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)
    * Check here if there are issues with the output format pattern.
    */
    private fun formatterPattern() : String {
        return "HH:mm:ss" + if (precision > 0) "." + "S".repeat(min(9, precision)) else ""
    }

    override fun toString(): String =
        localTime.format(DateTimeFormatter.ofPattern(formatterPattern())) +
            (zoneOffset?.getOffsetHHmm() ?: "")
}