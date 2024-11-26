package org.partiql.spi.datetime;

import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;

/**
 * A PartiQL time without timezone.
 * <br>
 * <bold>Usage</bold>
 * <pre>
 *     Time.of(time); // time instanceof java.time.LocalTime
 *     Time.of(10, 30);
 *     Time.of(10, 30, 0);
 * </pre>
 */
public class Time {

    /**
     * Delegate all functionality to {@link LocalTime}.
     */
    @NotNull
    private final LocalTime time;

    /**
     * NO PUBLIC CONSTRUCTORS.
     */
    private Time(@NotNull LocalTime time) {
        this.time = time;
    }

    /**
     * @return time for current date-time for the system clock.
     */
    @NotNull
    public static Time now() {
        return new Time(LocalTime.now());
    }

    /**
     * Create a Time from a {@link LocalTime}.
     *
     * @param time local time (without timezone).
     * @return new Time
     */
    @NotNull
    public static Time of(@NotNull LocalTime time) {
        return new Time(time);
    }

    /**
     * Create a Time from an hour, minute.
     *
     * @param hour   hour-of-day from 0 to 23
     * @param minute minute-of-hour from 0 to 59
     * @return new Time
     */

    @NotNull
    public static Time of(int hour, int minute) {
        return new Time(LocalTime.of(hour, minute));
    }

    /**
     * Create a Time from an hour, minute, second.
     *
     * @param hour   hour-of-day from 0 to 23
     * @param minute minute-of-hour from 0 to 59
     * @param second second-of-minute from 0 to 59
     * @return new Time
     */
    @NotNull
    public static Time of(int hour, int minute, int second) {
        return new Time(LocalTime.of(hour, minute, second));
    }

    /**
     *
     * @param hour   hour-of-day from 0 to 23
     * @param minute minute-of-hour from 0 to 59
     * @param second second-of-minute from 0 to 59
     * @param nano nano-of-second from 0 to 999,999,999
     * @return new Time
     */
    @NotNull
    public static Time of(int hour, int minute, int second, int nano) {
        return new Time(LocalTime.of(hour, minute, second, nano));
    }

    /**
     * @return a {@link LocalTime} for all functionality.
     */
    @NotNull
    public LocalTime toLocalTime() {
        return time;
    }

    /**
     * @return the hour-of-day, from 0 to 23
     */
    public int getHour() {
        return time.getHour();
    }

    /**
     * @return the minute-of-hour, from 0 to 59
     */
    public int getMinute() {
        return time.getMinute();
    }

    /**
     * @return the second-of-minute, from 0 to 59
     */
    public int getSecond() {
        return time.getSecond();
    }

    /**
     * @return the nano-of-second, from 0 to 999,999,999
     */
    public int getNano() {
        return time.getNano();
    }

    @Override
    public int hashCode() {
        return time.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Time))
            return false;
        return time.equals(((Time) obj).time);
    }

    /**
     * @return SQL string.
     */
    @Override
    public String toString() {
        // TODO confirm SQL FORMATTER
        return "TIME '" + time;
    }
}
