package org.partiql.lang.eval.builtins

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.err
import org.partiql.lang.util.getOffsetHHmm
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.times
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.min

/**
 * Wrapper class representing the run time instance of TIME in PartiQL.
 * - `TIME [(p)] HH:MM:ss[.ddd][+|-HH:MM]` PartiQL statement creates a run-time instance of this class with [zoneOffset] as [null].
 * - `TIME WITH TIME ZONE [(p)] HH:MM:ss[.ddd][+|-HH:MM]` PartiQL statement creates a run-time instance of this class with both
 * [localTime] and [zoneOffset] defined.
 *
 * @param localTime Represents the local time.
 * @param zoneOffset Represents the time zone of the TIME instance.
 *  If the [zoneOffset] is null, the zone offset is not defined for the instance.
 * @param precision Represents the number of significant digits in fractional part of the second's value of the TIME instance.
 *  The default precision is 9 meaning, the default precision is of nanoseconds.
 *  If the precision is specified by the user, the fractional part of the second will be rounded to the specified precision.
 *  For eg, `TIME (3) 23:46:58.1267` will be stored in this instance with [localTime] as `23:46:58.127000000` along with preserving the
 *  [precision] value as 3.
 *  Note that the [LocalTime] always stores the fractional part of the second in nanoseconds.
 *  It is up to the application developers to make use of the preserved [precision] value as need be.
 */
data class Time(val localTime: LocalTime, val zoneOffset: ZoneOffset? = null, val precision: Int = MAX_PRECISION_FOR_TIME) {

    init {
        // Validate that the precision value is non-negative.
        if (precision < 0) {
            err(
                message = "Specified precision for TIME should be a non-negative integer between 0 and 9 inclusive",
                errorCode = ErrorCode.EVALUATOR_INVALID_PRECISION_FOR_TIME,
                errorContext = propertyValueMapOf(),
                internal = false)
        }
    }

    companion object {

        const val PARTIQL_TIME_ANNOTATION = "\$partiql_time"
        const val HOURS_PER_DAY = 24
        const val MINUTES_PER_HOUR = 60
        const val SECONDS_PER_MINUTE = 60
        const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
        const val NANOS_PER_SECOND = 1000000000.0
        const val MAX_PRECISION_FOR_TIME = 9

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
        fun of(hour: Int, minute: Int, second: Int, nano: Int, precision: Int = MAX_PRECISION_FOR_TIME, tz_minutes: Int? = null) : Time {
            // Round nanoseconds to the given precision.
            val nanoWithPrecision = when  {
                precision >= 9 -> nano
                else -> (BigDecimal(nano / NANOS_PER_SECOND).setScale(precision, RoundingMode.HALF_UP) * NANOS_PER_SECOND).toInt()
            }
            // If the nanos are added to form up a whole second because of the specified precision, carry over the second all up to the hour
            // and use the mod values of all the new fields to fit in the valid range.
            val newNano = nanoWithPrecision % NANOS_PER_SECOND.toInt()
            val newSecond = second + (nanoWithPrecision / NANOS_PER_SECOND.toInt())
            val newMinute = minute + (newSecond / SECONDS_PER_MINUTE)
            val newHour = (hour + (newMinute / MINUTES_PER_HOUR))
            val localTime = LocalTime.of(
                newHour % HOURS_PER_DAY,
                newMinute % MINUTES_PER_HOUR,
                newSecond % SECONDS_PER_MINUTE,
                newNano
            )
            val zoneOffset = tz_minutes?.let {
                ZoneOffset.ofTotalSeconds( it * SECONDS_PER_MINUTE)
            }
            return Time(localTime, zoneOffset, precision)
        }
    }

    val offsetTime
        get() : OffsetTime? = zoneOffset?.let {
            OffsetTime.of(localTime, it)
        }

    fun toIonValue(ion: IonSystem): IonStruct =
        ion.newEmptyStruct().apply {
            add("hour", ion.newInt(localTime.hour))
            add("minute", ion.newInt(localTime.minute))
            add("second", ion.newDecimal(BigDecimal(localTime.second + localTime.nano / NANOS_PER_SECOND).setScale(precision, RoundingMode.HALF_UP)))
            add("timezone_hour", ion.newInt(zoneOffset?.totalSeconds?.div(SECONDS_PER_HOUR)))
            add("timezone_minute", ion.newInt(zoneOffset?.totalSeconds?.div((SECONDS_PER_MINUTE))?.rem(60)))
            addTypeAnnotation(PARTIQL_TIME_ANNOTATION)
        }

    private fun formatterPattern() : String {
        return "HH:mm:ss" + if (precision > 0) "." + "S".repeat(min(9, precision)) else ""
    }

    override fun toString(): String =
        localTime.format(DateTimeFormatter.ofPattern(formatterPattern())) +
            (zoneOffset?.getOffsetHHmm() ?: "")
}