package org.partiql.spi.datetime;


import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * A PartiQL timestamp without timezone.
 */
public class Timestamp {

    private final Date date;
    private final Time time;

    /**
     * NO PUBLIC CONSTRUCTORS.
     */
    private Timestamp(@NotNull Date date, @NotNull Time time) {
        this.date = date;
        this.time = time;
    }

    /**
     * @return timestamp for current date-time for the system clock.
     */
    @NotNull
    public Timestamp now() {
        return new Timestamp(Date.now(), Time.now());
    }

    /**
     * Create a new Timestamp from the date-time pair.
     *
     * @param date a date.
     * @param time a time.
     * @return new Timestamp
     */
    @NotNull
    public static Timestamp of(@NotNull Date date, @NotNull Time time) {
        return new Timestamp(date, time);
    }

    /**
     * Create a new Timestamp from the fields.
     *
     * @param year   the year to represent, from Year.MIN_YEAR to Year.MAX_YEAR
     * @param month  the month-of-year to represent, from 1 (January) to 12 (December)
     * @param day    the day of month to represent, from 1 to 31
     * @param hour   hour-of-day from 0 to 23
     * @param minute minute-of-hour from 0 to 59
     * @param second second-of-minute from 0 to 59
     * @return new Timestamp
     */
    @NotNull
    public static Timestamp of(int year, int month, int day, int hour, int minute, int second) {
        Date date = Date.of(year, month, day);
        Time time = Time.of(hour, minute, second);
        return new Timestamp(date, time);
    }

    /**
     * @param year   the year to represent, from Year.MIN_YEAR to Year.MAX_YEAR
     * @param month  the month-of-year to represent, from 1 (January) to 12 (December)
     * @param day    the day of month to represent, from 1 to 31
     * @param hour   hour-of-day from 0 to 23
     * @param minute minute-of-hour from 0 to 59
     * @param second second-of-minute from 0 to 59
     * @param nano   nano-of-second from 0 to 999,999,999
     * @return new Timestamp
     */
    @NotNull
    public static Timestamp of(int year, int month, int day, int hour, int minute, int second, int nano) {
        Date date = Date.of(year, month, day);
        Time time = Time.of(hour, minute, second, nano);
        return new Timestamp(date, time);
    }

    /**
     * Create a Timestamp from a {@link LocalDateTime}.
     *
     * @param timestamp local date time (without timezone).
     * @return new Timestamp
     */
    @NotNull
    public static Timestamp of(@NotNull LocalDateTime timestamp) {
        return new Timestamp(Date.of(timestamp.toLocalDate()), Time.of(timestamp.toLocalTime()));
    }

    /**
     * @return a {@link LocalDateTime} for this timestamp.
     */
    @NotNull
    public LocalDateTime toLocalDateTime() {
        return LocalDateTime.of(date.toLocalDate(), time.toLocalTime());
    }

    /**
     * @return the timestamp date.
     */
    @NotNull
    public Date toDate() {
        return date;
    }

    /**
     * @return the timestamp time.
     */
    @NotNull
    public Time toTime() {
        return time;
    }

    /**
     * @return the year of this date
     */
    public int getYear() {
        return date.getYear();
    }

    /**
     * @return the month (of the year) field.
     */
    public int getMonth() {
        return date.getMonth();
    }

    /**
     * @return the day-of-month field.
     */
    public int getDay() {
        return date.getDay();
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
        return toLocalDateTime().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Timestamp)) return false;
        Timestamp o = (Timestamp) obj;
        return time.equals(o.time) && date.equals(o.date);
    }

    @Override
    public String toString() {
        // TODO confirm SQL format
        return "TIMESTAMP " + date + " " + time;
    }
}
