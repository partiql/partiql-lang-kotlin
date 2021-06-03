package org.partiql.lang.eval.time

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errNoContext
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
internal const val NANOS_PER_SECOND = 1000000000
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
 *  The fractional part of the second will be rounded to the specified precision.
 *  For eg, `TIME (3) 23:46:58.1267` will be stored in this instance with [localTime] as `23:46:58.127000000` along with preserving the
 *  [precision] value as 3.
 *  Note that the [LocalTime] always stores the fractional part of the second in nanoseconds.
 *  It is up to the application developers to make use of the preserved [precision] value as need be.
 */
data class Time private constructor(val localTime: LocalTime, val precision: Int, val zoneOffset: ZoneOffset? = null) {

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

        private const val LESS = -1
        private const val MORE = 1

        /** Returns an instance of [Time] for the given hour, minute, second, precision and tz_minutes.
         * @param hour  the hour of a day of 24 hours to represent, from 0 to 23
         * @param minute  the minute of hour of 60 minutes to represent, from 0 to 59
         * @param second  the second of minute of 60 seconds to represent, from 0 to 59
         * @param nano  the nano of second to represent, from 0 to 999,999,999.
         * @param precision  the number of desired significant digits in the fractional part of the second's value.
         * If the precision is less than 9, the fractional part of the second will be rounded to the precision and [nano] will store the rounded value.
         * For e.g., if [nano] is 126700000 and the [precision] is 3, the [nano] will be rounded to 127000000.
         * Note that the [nano] will still store the value in nanoseconds (0's padded after the desired precision digits),
         * however the [Time] instance will preserve the [precision] thereby preserving the entire original value.
         * The valid values for precision are between 0 and 9 inclusive.
         * @param tz_minutes  the minutes of the UTC time-zone offset, from -1080 to 1080.
         * If [tz_minutes] is null then the timezone offset is not defined.
         * @return TimeExprValue
         * @throws EvaluationException if the value of any field is out of range
         */
        @JvmStatic
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
                throw EvaluationException(dte, ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE, propertyValueMapOf(), false)
            }

            // Round nanoseconds to the given precision.
            val nanoWithPrecision = when (precision) {
                MAX_PRECISION_FOR_TIME -> nano
                else -> ((nano.toBigDecimal().divide(NANOS_PER_SECOND.toBigDecimal())).setScale(precision, RoundingMode.HALF_EVEN).multiply(NANOS_PER_SECOND.toBigDecimal())).toInt()
            }
            // If the nanos are added to form up a whole second because of the specified precision, carry over the second all up to the hour
            // and use the mod values of all the new fields to fit in the valid range.
            val newNano = nanoWithPrecision % NANOS_PER_SECOND
            val newSecond = second + (nanoWithPrecision / NANOS_PER_SECOND)
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
                ZoneOffset.ofTotalSeconds(it * SECONDS_PER_MINUTE)
            }
            return Time(localTime, precision, zoneOffset)
        }

        /**
         * Returns an instance of [Time] for the given localTime, precision and zoneOffset.
         * Precision is used to round up the fractional part of the second (nano field of localTime).
         * The [Time] instance returned has the [LocalTime] rounded up to this precision.
         */
        @JvmStatic
        @JvmOverloads
        fun of(localTime: LocalTime, precision: Int, zoneOffset: ZoneOffset? = null) : Time {
            return Time.of(localTime.hour, localTime.minute, localTime.second, localTime.nano, precision,
                zoneOffset?.totalSeconds?.div(SECONDS_PER_MINUTE)
            )
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
    val secondsWithFractionalPart : BigDecimal
        get()  = (localTime.second.toBigDecimal() + localTime.nano.toBigDecimal().divide(NANOS_PER_SECOND.toBigDecimal()))
                    .setScale(precision, RoundingMode.HALF_EVEN)

    fun toIonValue(ion: IonSystem): IonStruct =
        ion.newEmptyStruct().apply {
            add("hour", ion.newInt(localTime.hour))
            add("minute", ion.newInt(localTime.minute))
            add("second", ion.newDecimal(secondsWithFractionalPart))
            add("timezone_hour", ion.newInt(timezoneHour))
            add("timezone_minute", ion.newInt(timezoneMinute))
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

    /**
     * Check if this instance is directly comparable to the other [Time] instance.
     * The [Time] instances are directly comparable if [zoneOffset] is defined for both of them
     * or it is not defined for both of them.
     */
    fun isDirectlyComparableTo(other: Time): Boolean {
        return (this.zoneOffset == null && other.zoneOffset == null) ||
            (this.zoneOffset != null && other.zoneOffset != null)
    }

    /**
     * Compares the TIME and TIME WITH TIME ZONE values according to the natural order.
     * TIME (without time zone) comes before TIME (with time zone) in the natural order comparison.
     */
    fun naturalOrderCompareTo(other: Time): Int {
        return when {
            // When the zone offsets are not null for both the operands, compare OffsetTime i.e. LocalTime with ZoneOffset
            this.zoneOffset != null && other.zoneOffset != null -> this.offsetTime!!.compareTo(other.offsetTime)
            // When the zone offsets are null for both the operands, compare just LocalTime
            this.zoneOffset == null && other.zoneOffset == null -> this.localTime.compareTo(other.localTime)
            // When one of the times is `time with time zone` and other is just `time` (without time zone), then they are incomparable.
            this.zoneOffset == null && other.zoneOffset != null -> LESS
            else -> MORE
        }
    }
}