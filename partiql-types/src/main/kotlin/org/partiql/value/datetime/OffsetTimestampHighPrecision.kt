package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_DAY
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_MINUTE
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.jvm.Throws
import com.amazon.ion.Timestamp as TimestampIon

public data class OffsetTimestampHighPrecision private constructor(
    override val year: Int,
    override val month: Int,
    override val day: Int,
    override val hour: Int,
    override val minute: Int,
    override val decimalSecond: BigDecimal,
    override val timeZone: TimeZone
) : TimestampWithTimeZone {
    public companion object {
        /**
         * Construct a timestamp value using date time field and a given precision.
         *
         * @param year Year field
         * @param month Month field
         * @param day Day field
         * @param hour Hour field
         * @param second Second field, include fraction decimalSecond
         * @param timeZone TimeZone field, see [TimeZone], null value indicates a timestamp without timezone value.
         * @param precision If value is null, the timestamp has arbitrary precision.
         *                  If the value is non-null, the timestamp has a fixed precision.
         *                  (precision means number of digits in fraction decimalSecond.)
         */
        public fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: BigDecimal,
            timeZone: TimeZone
        ): OffsetTimestampHighPrecision {
            val date = SqlDate.of(year, month, day)
            val time = OffsetTimeHighPrecision.of(hour, minute, second, timeZone)
            return forDateTime(date, time)
        }

        /**
         * The intention of this API is to create a Timestamp using a Date and a Time component.
         * It is assumed that the time component has already been rounded,
         * meaning this API will not be responsible, if the rounding requires carrying in to day field.
         *
         * For example: the result of
         * Timestamp.of(Date.of(2023, 06, 01), Time.of(23, 59, 59.9, TimeZone.of(0,0), 0)
         * will result in
         * 2023-06-01T00:00:00+00:00
         *
         * If the desired result is `2023-06-02T00:00:00+00:00`, use [of]
         */
        @JvmStatic
        public fun forDateTime(date: Date, time: TimeWithTimeZone): OffsetTimestampHighPrecision =
            OffsetTimestampHighPrecision(
                date.year, date.month, date.day,
                time.hour, time.minute, time.decimalSecond.toBigDecimal(),
                time.timeZone
            )

        /**
         * Construct a PartiQL timestamp based on an Ion Timestamp.
         * The created timestamp always has [TimeZone] and arbitrary precision.
         * Notice that Ion Value allows for "lower precision", year, month, etc.
         * For example, `2023T` is a valid ion timestamp with year precision.
         * This method always returns a "full timestamp expression", i.e., 2023-01-01T00:00:00.
         * At the moment there is no intention on preserving this.
         */
        @JvmStatic
        public fun forIonTimestamp(ionTs: com.amazon.ion.Timestamp): OffsetTimestampHighPrecision {
            val timestamp = when (ionTs.localOffset) {
                null ->
                    OffsetTimestampHighPrecision(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UnknownTimeZone,
                    )

                else ->
                    OffsetTimestampHighPrecision(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UtcOffset.of(ionTs.localOffset),
                    )
            }
            return timestamp.let {
                it.ionRaw = ionTs
                it
            }
        }

        /**
         * Returns a timestamp based on epoch decimalSecond.
         *
         * The resulting timestamp is always a timestamp with timezone,
         * this is because epoch decimalSecond by definition refers to a point in time.
         * and timestamp without time zone does not refer to a point forEpochSecond time.
         */
        @JvmStatic
        public fun forEpochSecond(
            seconds: BigDecimal,
            timeZone: TimeZone = TimeZone.UnknownTimeZone,
            precision: Int? = null
        ): OffsetTimestampHighPrecision {
            val offsetDateTime =
                java.time.Instant.ofEpochSecond(seconds.setScale(0, RoundingMode.DOWN).longValueExact()).atOffset(
                    ZoneOffset.UTC
                )
            val year = offsetDateTime.year
            val month = offsetDateTime.monthValue
            val day = offsetDateTime.dayOfMonth
            val hour = offsetDateTime.hour
            val minute = offsetDateTime.minute
            val wholeSecond = offsetDateTime.second
            val fractionSecond =
                seconds.minus(BigDecimal.valueOf(seconds.setScale(0, RoundingMode.DOWN).longValueExact()))
            return of(
                year, month, day,
                hour, minute,
                fractionSecond.add(BigDecimal.valueOf(wholeSecond.toLong())),
                timeZone,
            )
        }

        @JvmStatic
        public fun nowZ(): Timestamp =
            forEpochSecond(BigDecimal.valueOf(System.currentTimeMillis(), 3), TimeZone.UtcOffset.of(0))
    }

    /**
     * Returns an Ion Equivalent Timestamp
     * [ionTimestampValue] takes care of the precision,
     * the ion representation will have exact precision of the backing partiQL value.
     */
    @get:Throws(DateTimeException::class)
    public val ionTimestampValue: TimestampIon by lazy {
        when (val timeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> TimestampIon.forSecond(
                this.year, this.month, this.day,
                this.hour, this.minute, this.decimalSecond,
                TimestampIon.UNKNOWN_OFFSET
            )

            is TimeZone.UtcOffset -> TimestampIon.forSecond(
                this.year, this.month, this.day,
                this.hour, this.minute, this.decimalSecond,
                timeZone.totalOffsetMinutes
            )
        }
    }

    /**
     * Returns a BigDecimal representing the Timestamp's point in time that is
     * the number of Seconds (*including* any fractional Seconds)
     * from the epoch.
     *
     * Since PartiQL support a wider range than Unix Time,
     * timestamp value that is before 1970-01-01T00:00:00Z will have a negative epoch value.
     *
     * If a timestamp does not contain Information regarding timezone,
     * there is no way to assign a point in time for such value, therefore the method will throw an error.
     *
     * If a timestamp contains unknown timezone, by semantics its UTC value is known, we return the UTC value in epoch.
     *
     * This method will return the same result for all Timestamp values representing
     * the same point in time, regardless of the local offset.
     */
    val epochSecond: BigDecimal by lazy {
        when (val timeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> getUTCEpoch(0)
            is TimeZone.UtcOffset -> getUTCEpoch(timeZone.totalOffsetMinutes)
        }
    }

    val epochMillis: BigDecimal by lazy {
        epochSecond.movePointRight(3)
    }

    val date: Date = SqlDate.of(this.year, this.month, this.day)
    val time: OffsetTimeHighPrecision = OffsetTimeHighPrecision.of(this.hour, this.minute, this.decimalSecond, this.timeZone)

    /**
     * For backward compatibility issue, we track the original Ion Input
     */
    @Deprecated(
        "We will not store raw Ion Timestamp Value in the next release.",
        replaceWith = ReplaceWith("ionTimestampValue")
    )
    var ionRaw: com.amazon.ion.Timestamp? = null


    override fun plusYear(years: Long): OffsetTimestampHighPrecision = forDateTime(this.date.plusYear(years), this.time)

    override fun plusMonths(months: Long): OffsetTimestampHighPrecision =
        forDateTime(this.date.plusMonths(months), this.time)

    override fun plusDays(days: Long): OffsetTimestampHighPrecision = forDateTime(this.date.plusDays(days), this.time)

    override fun plusHours(hours: Long): OffsetTimestampHighPrecision = forEpochSecond(
        this.epochSecond.plus((hours * DateTimeUtil.SECONDS_IN_HOUR).toBigDecimal()),
        timeZone
    )

    override fun plusMinutes(minutes: Long): OffsetTimestampHighPrecision =
        forEpochSecond(this.epochSecond.plus((minutes * SECONDS_IN_MINUTE).toBigDecimal()), timeZone)

    override fun plusSeconds(seconds: Number): OffsetTimestampHighPrecision =
        forEpochSecond(this.epochSecond.plus(seconds.toBigDecimal()), timeZone)

    override fun atTimeZone(timeZone: TimeZone): OffsetTimestampHighPrecision =
        when (val valueTimeZone = this.timeZone) {
            // Unknown TimeZone : Then we need to modify the
            TimeZone.UnknownTimeZone -> this.atTimeZone(TimeZone.UtcOffset.of(0)).atTimeZone(timeZone)
            is TimeZone.UtcOffset -> {
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> this.atTimeZone(TimeZone.UtcOffset.of(0)).copy(timeZone = timeZone)
                    is TimeZone.UtcOffset -> {
                        var _minute = this.minute - valueTimeZone.tzMinute + timeZone.tzMinute
                        var _hour = this.hour - valueTimeZone.tzHour + valueTimeZone.tzHour
                        var hourOffset = _minute / 60
                        _minute %= 60
                        if (_minute < 0) {
                            _minute += 60
                            hourOffset -= 1
                        }
                        _hour += hourOffset
                        var dayOffset = _hour / 24
                        _hour %= 24
                        if (_hour < 0) {
                            _hour += 24
                            dayOffset -= 1
                        }
                        forDateTime(
                            SqlDate.of(this.year, this.month, this.day).plusDays(dayOffset.toLong()),
                            OffsetTimeHighPrecision.of(_hour, _minute, this.decimalSecond, timeZone)
                        )
                    }
                }
            }
        }

    override fun toPrecision(precision: Int): Timestamp {
        TODO("Not yet implemented")
    }

    override fun toDate(): Date {
        TODO("Not yet implemented")
    }

    override fun toTime(): Time {
        TODO("Not yet implemented")
    }

    private fun getUTCEpoch(totalOffsetMinutes: Int): BigDecimal {
        val epochDay = LocalDate.of(year, month, day).toEpochDay()
        // Deal with time zone first so we delay big decimal op
        val adjuestForTimeZoneInSecond = epochDay * SECONDS_IN_DAY - totalOffsetMinutes * DateTimeUtil.SECONDS_IN_MINUTE

        return BigDecimal.valueOf(adjuestForTimeZoneInSecond).plus(this.time.elapsedSecond)
    }

}