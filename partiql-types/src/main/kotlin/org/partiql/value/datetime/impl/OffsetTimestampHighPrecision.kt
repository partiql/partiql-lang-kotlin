package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_DAY
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_HOUR
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_MINUTE
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneOffset

internal class OffsetTimestampHighPrecision private constructor(
    override val year: Int,
    override val month: Int,
    override val day: Int,
    override val hour: Int,
    override val minute: Int,
    override val decimalSecond: BigDecimal,
    override val timeZone: TimeZone,
    _inputIonTimestamp: com.amazon.ion.Timestamp? = null,
    _epochSecond: BigDecimal? = null,
    _date: Date?,
    _time: TimeWithTimeZone?,
) : TimestampWithTimeZone() {
    companion object {
        /**
         * Construct a timestamp value using date time field and a given precision.
         *
         * @param year Year field
         * @param month Month field
         * @param day Day field
         * @param hour Hour field
         * @param second Second field, include fraction decimalSecond
         * @param timeZone TimeZone field, see [TimeZone], null value indicates a timestamp without timezone value.
         */
        fun of(
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
         */
        @JvmStatic
        fun forDateTime(date: Date, time: TimeWithTimeZone): OffsetTimestampHighPrecision =
            OffsetTimestampHighPrecision(
                date.year, date.month, date.day,
                time.hour, time.minute, time.decimalSecond.toBigDecimal(),
                time.timeZone, null, null, date, time
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
        fun forIonTimestamp(ionTs: com.amazon.ion.Timestamp): OffsetTimestampHighPrecision {
            val timestamp = when (ionTs.localOffset) {
                null ->
                    OffsetTimestampHighPrecision(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UnknownTimeZone, ionTs,
                        null, null, null
                    )

                else ->
                    OffsetTimestampHighPrecision(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UtcOffset.of(ionTs.localOffset), ionTs,
                        null, null, null
                    )
            }
            return timestamp
        }

        /**
         * Returns a timestamp based on epoch decimalSecond.
         *
         * The resulting timestamp is always a timestamp with timezone,
         * this is because epoch decimalSecond by definition refers to a point in time.
         * and timestamp without time zone does not refer to a point forEpochSecond time.
         */
        @JvmStatic
        fun forEpochSecond(
            epochSeconds: BigDecimal,
            timeZone: TimeZone = TimeZone.UnknownTimeZone,
        ): OffsetTimestampHighPrecision {
            val offsetDateTime =
                java.time.Instant.ofEpochSecond(epochSeconds.setScale(0, RoundingMode.DOWN).longValueExact()).atOffset(
                    ZoneOffset.UTC
                )
            val year = offsetDateTime.year
            val month = offsetDateTime.monthValue
            val day = offsetDateTime.dayOfMonth
            val hour = offsetDateTime.hour
            val minute = offsetDateTime.minute
            val wholeSecond = offsetDateTime.second
            val fractionSecond =
                epochSeconds.minus(BigDecimal.valueOf(epochSeconds.setScale(0, RoundingMode.DOWN).longValueExact()))
            return of(
                year, month, day,
                hour, minute,
                fractionSecond.add(BigDecimal.valueOf(wholeSecond.toLong())),
                timeZone
            ).let {
                OffsetTimestampHighPrecision(
                    it.year, it.month, it.day,
                    it.hour, it.minute, it.decimalSecond,
                    it.timeZone, null,
                    epochSeconds,null, null
                )
            }
        }
    }

    @Deprecated("We will not store raw Ion Timestamp Value in the next release.")
    override val ionRaw: com.amazon.ion.Timestamp? = _inputIonTimestamp
    override val epochSecond: BigDecimal by lazy {
        _epochSecond ?: when (val timeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> getUTCEpoch(0)
            is TimeZone.UtcOffset -> getUTCEpoch(timeZone.totalOffsetMinutes)
        }
    }
    internal val date = _date ?: SqlDate.of(year, month, day)
    internal val time = _time ?: OffsetTimeHighPrecision.of(this.hour, this.minute, this.decimalSecond, this.timeZone)

    override fun plusYear(years: Long): TimestampWithTimeZone =
        forDateTime(this.date.plusYear(years), this.time)

    override fun plusMonths(months: Long): TimestampWithTimeZone =
        forDateTime(this.date.plusMonths(months), this.time)

    override fun plusDays(days: Long): TimestampWithTimeZone =
        forDateTime(this.date.plusDays(days), this.time)

    override fun plusHours(hours: Long): TimestampWithTimeZone =
        forEpochSecond(this.epochSecond.plus((hours * SECONDS_IN_HOUR).toBigDecimal()), timeZone)

    override fun plusMinutes(minutes: Long): TimestampWithTimeZone =
        forEpochSecond(this.epochSecond.plus((minutes * SECONDS_IN_MINUTE).toBigDecimal()), timeZone)

    override fun plusSeconds(seconds: BigDecimal): TimestampWithTimeZone =
        forEpochSecond(this.epochSecond.plus(seconds.toBigDecimal()), timeZone)

    override fun toTimeWithoutTimeZone(timeZone: TimeZone): TimestampWithoutTimeZone =
        this.atTimeZone(timeZone).let {
            LocalTimestampHighPrecision.of(it.year, it.month, it.day, it.hour, it.minute, it.decimalSecond)
        }

    override fun atTimeZone(timeZone: TimeZone): TimestampWithTimeZone =
        when (val valueTimeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> {
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> this
                    is TimeZone.UtcOffset -> of(
                        year, month, day, hour, minute, decimalSecond, TimeZone.UtcOffset.of(0)
                    ).atTimeZone(timeZone)
                }
            }

            is TimeZone.UtcOffset -> {
                val utc = this.plusMinutes(-valueTimeZone.totalOffsetMinutes.toLong())
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> of(
                        utc.year, utc.month, utc.day,
                        utc.hour, utc.minute, utc.decimalSecond,
                        timeZone
                    )
                    is TimeZone.UtcOffset -> utc.plusMinutes(timeZone.totalOffsetMinutes.toLong())
                        .let { of(it.year, it.month, it.day, it.hour, it.minute, it.decimalSecond, timeZone) }
                }
            }
        }

    private fun getUTCEpoch(totalOffsetMinutes: Int): BigDecimal {
        val epochDay = LocalDate.of(year, month, day).toEpochDay()
        // Deal with time zone first, so we delay big decimal op
        val adjustForTimeZoneInSecond = epochDay * SECONDS_IN_DAY - totalOffsetMinutes * SECONDS_IN_MINUTE

        return BigDecimal.valueOf(adjustForTimeZoneInSecond).plus(this.time.elapsedSecond)
    }

    internal fun copy(_inputIonTimestamp: com.amazon.ion.Timestamp? = null, _epochSecond: BigDecimal? = null,
                      _date: Date? = null, _time: TimeWithTimeZone? = null) =
        OffsetTimestampHighPrecision(
            this.year, this.month, this.day,
            this.hour, this.minute, this.decimalSecond, this.timeZone,
            _inputIonTimestamp?: this.ionTimestampValue, _epochSecond?: this.epochSecond,
            _date?: this.date, _time?: this.time
        )
}