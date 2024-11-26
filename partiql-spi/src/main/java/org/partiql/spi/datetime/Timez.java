package org.partiql.spi.datetime;

import jdk.vm.ci.meta.Local;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

/**
 * A PartiQL time with a UTC-offset timezone.
 * <br>
 * <bold>Usage</bold>
 * <pre>
 *     Timez.of(time); // time instanceof java.time.OffsetTime
 *     Timez.of(10, 30);
 *     Timez.of(10, 30, 0);
 * </pre>
 */
public class Timez {

    /**
     * Delegate all functionality to {@link OffsetTime}.
     */
    @NotNull
    private final OffsetTime time;

    /**
     * NO PUBLIC CONSTRUCTORS.
     */
    private Timez(@NotNull OffsetTime time) {
        this.time = time;
    }

    /**
     * @return timez for current date-time for the system clock.
     */
    @NotNull
    public static Timez now() {
        return new Timez(OffsetTime.now());
    }

    /**
     * Create a Timez from a {@link OffsetTime}.
     *
     * @param time offset time (with timezone).
     * @return new Timez
     */
    @NotNull
    public static Timez of(@NotNull OffsetTime time) {
        return new Timez(time);
    }

    /**
     * Create a Timez from a {@link LocalTime} and {@link ZoneOffset}.
     *
     * @param time local time (without timezone).
     * @param offset timezone offset
     * @return new Timez
     */
    @NotNull
    public static Timez of(@NotNull LocalTime time, @NotNull ZoneOffset offset) {
        return new Timez(OffsetTime.of(time, offset));
    }

    /**
     * @return a {@link LocalTime}
     */
    @NotNull
    public LocalTime toLocalTime() {
        return time.toLocalTime();
    }

    /**
     * @return an {@link OffsetTime}
     */
    @NotNull
    public OffsetTime toOffsetTime() {
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

    @NotNull
    public ZoneOffset getOffset() {
        return time.getOffset();
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
        if (!(obj instanceof Timez))
            return false;
        return time.equals(((Timez) obj).time);
    }

    /**
     * @return SQL string.
     */
    @Override
    public String toString() {
        // TODO confirm SQL FORMATTER
        return "TIMEZ '" + time;
    }
}
