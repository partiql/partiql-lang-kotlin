package org.partiql.spi.datetime;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Period;

/**
 * This class represents a PartiQL Interval value. SQL defines two "classes" of intervals: year-month and day-time.
 * In Java these are represented by {@link Period} and {@link Duration} respectively.
 */
public interface Interval {

    /**
     * A {@link Period} based interval for the year-month interval class.
     */
    public class YM implements Interval {

        /**
         * The interval year, month, day (calendar aware).
         */
        private final Period period;

        /**
         * @param period a year-month INTERVAL part.
         */
        private YM(@NotNull Period period) {
            this.period = period;
        }

        /**
         * @return this interval as a Period.
         */
        @NotNull
        public Period toPeriod() {
            return period;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof YM))
                return false;
            return period.equals(((YM) obj).period);
        }

        @Override
        public int hashCode() {
            return period.hashCode();
        }

        @Override
        public String toString() {
            return period.toString();
        }
    }

    /**
     * A  {@link Duration} based interval for the day-time interval class.
     */
    public class DT implements Interval {

        /**
         * The seconds, nanoseconds of the interval (calendar unaware).
         */
        @NotNull
        private final Duration duration;

        /**
         * @param duration a day-time INTERVAL part.
         */
        private DT(@NotNull Duration duration) {
            this.duration = duration;
        }


        /**
         * @return this interval as a Duration.
         */
        @NotNull
        public Duration toDuration() {
            return duration;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof DT))
                return false;
            return duration.equals(((DT) obj).duration);
        }

        @Override
        public int hashCode() {
            return duration.hashCode();
        }

        @Override
        public String toString() {
            return duration.toString();
        }
    }

    @NotNull
    public static YM period(Period period) {
        return new YM(period);
    }

    @NotNull
    public static DT duration(Duration duration) {
        return new DT(duration);
    }

    /**
     * Create an interval based on the number of years.
     *
     * @param years number of years
     * @return new Interval
     */
    @NotNull
    public static YM years(int years) {
        return new YM(Period.ofYears(years));
    }

    /**
     * Create an interval based on the number of months.
     *
     * @param months number of months
     * @return new Interval
     */
    @NotNull
    public static YM months(int months) {
        return new YM(Period.ofMonths(months));
    }

    /**
     * Create an interval based on the number of weeks.
     *
     * @param weeks number of weeks
     * @return new Interval
     */
    @NotNull
    public static YM weeks(int weeks) {
        return new YM(Period.ofWeeks(weeks));
    }

    /**
     * Create an interval based on the number of days.
     *
     * @param days number of days
     * @return new Interval
     */
    @NotNull
    public static YM days(int days) {
        return new YM(Period.ofDays(days));
    }

    /**
     * Create an interval based on the number of hours.
     *
     * @param hours number of hours
     * @return new Interval
     */
    @NotNull
    public static DT hours(int hours) {
        return new DT(Duration.ofHours(hours));
    }

    /**
     * Create an interval based on the number of minutes.
     *
     * @param minutes number of minutes
     * @return new Interval
     */
    @NotNull
    public static DT minutes(int minutes) {
        return new DT(Duration.ofMinutes(minutes));
    }

    /**
     * Create an interval based on the number of seconds.
     *
     * @param seconds number of seconds
     * @return new Interval
     */
    @NotNull
    public static DT seconds(long seconds) {
        return new DT(Duration.ofSeconds(seconds));
    }

    /**
     * Create an interval based on the number milliseconds.
     * @param milliseconds number of milliseconds
     * @return new Interval
     */
    @NotNull
    public static DT millis(long milliseconds) {
        return new DT(Duration.ofMillis(milliseconds));
    }

    /**
     * Create an interval based on the number nanoseconds.
     * @param nanoseconds number of nanoseconds
     * @return new Interval
     */
    @NotNull
    public static DT nanos(long nanoseconds) {
        return new DT(Duration.ofNanos(nanoseconds));
    }
}
