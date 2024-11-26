package org.partiql.spi.datetime;


import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * A PartiQL timestamp without timezone.
 */
public class Timestampz {

    private final Date date;
    private final Timez time;

    /**
     * NO PUBLIC CONSTRUCTORS.
     */
    private Timestampz(@NotNull Date date, @NotNull Timez time) {
        this.date = date;
        this.time = time;
    }

    /**
     * @return timestamp for current date-time for the system clock.
     */
    @NotNull
    public Timestampz now() {
        return new Timestampz(Date.now(), Timez.now());
    }

    /**
     * Create a new Timestampz from the date-time pair.
     *
     * @param date a date.
     * @param time a time.
     * @return new Timestampz
     */
    @NotNull
    public static Timestampz of(@NotNull Date date, @NotNull Timez time) {
        return new Timestampz(date, time);
    }

    /**
     * Create a Timestampz from a {@link LocalDateTime}.
     *
     * @param timestamp local date time (without timezone).
     * @return new Timestampz
     */
    @NotNull
    public static Timestampz of(@NotNull OffsetDateTime timestamp) {
        return new Timestampz(Date.of(timestamp.toLocalDate()), Timez.of(timestamp.toOffsetTime()));
    }

    /**
     * @return a {@link LocalDateTime}.
     */
    @NotNull
    public LocalDateTime toLocalDateTime() {
        return LocalDateTime.of(date.toLocalDate(), time.toLocalTime());
    }

    /**
     * @return an {@link OffsetDateTime}.
     */
    @NotNull
    public OffsetDateTime toOffsetDateTime() {
        return OffsetDateTime.of(toLocalDateTime(), time.getOffset());
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
    public Timez toTimez() {
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


    @NotNull
    public ZoneOffset getOffset() {
        return time.getOffset();
    }

    @Override
    public int hashCode() {
        return toLocalDateTime().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Timestampz)) return false;
        Timestampz o = (Timestampz) obj;
        return time.equals(o.time) && date.equals(o.date);
    }

    @Override
    public String toString() {
        // TODO confirm SQL format
        return "TIMESTAMPZ " + date + " " + time;
    }
}
