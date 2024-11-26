package org.partiql.spi.datetime;

import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * A PartiQL date.
 * <br>
 * This class provides basic APIs for creating a Date. More complex use-cases should create a Date from a LocalDate.
 * For example, to create a `now()` Date with an alternative timezone, use:
 * <pre>
 *     Clock clock = Clock.system(ZoneId.of("America/New_York"));
 *     Date date = new Date(LocalDate.now(clock));
 * </pre>
 * This API may be extended with additional constructors to reduce verbosity, but it is intentionally lean.
 */
public class Date {

    /**
     * Delegate all functionality to the JDK8+ recommend APIs, but keep this internalized.
     */
    @NotNull
    private final LocalDate date;

    /**
     * SQL date formatter.
     */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * NO PUBLIC CONSTRUCTORS.
     */
    private Date(@NotNull LocalDate date) {
        this.date = date;
    }

    /**
     * @return date for current date-time for the system clock.
     */
    @NotNull
    public static Date now() {
        return new Date(LocalDate.now(Clock.systemDefaultZone()));
    }

    /**
     * Create a new Date from a {@link LocalDate}.
     *
     * @param date the {@link LocalDate} to wrap
     */
    @NotNull
    public static Date of(@NotNull LocalDate date) {
        return new Date(date);
    }

    /**
     * Create a new Date from a year, month, and day.
     *
     * @param year  the year to represent, from Year.MIN_YEAR to Year.MAX_YEAR
     * @param month the month-of-year to represent, from 1 (January) to 12 (December)
     * @param day   the day of month to represent, from 1 to 31
     */
    @NotNull
    public static Date of(int year, int month, int day) {
        return new Date(LocalDate.of(year, month, day));
    }

    /**
     * @return a {@link LocalDate} representation of this date.
     */
    @NotNull
    public LocalDate toLocalDate() {
        return date;
    }

    /**
     * @return the year of this date
     */
    public int getYear() {
        return date.getYear();
    }

    /**
     * @return the month (of the year) field value.
     */
    public int getMonth() {
        return date.getMonthValue();
    }

    /**
     * @return the day (of the month) field value.
     */
    public int getDay() {
        return date.getDayOfMonth();
    }

    /**
     * @return temporal difference between this date and the other date.
     */
    @NotNull
    public Interval minus(Date other) {
        // inverse of 'until'
        return Interval.period(other.date.until(this.date));
    }

    /**
     * @return a new Date representing the result of adding the interval to this Date
     */
    @NotNull
    public Date plus(Interval interval) {
        LocalDate date;
        if (interval instanceof Interval.YM) {
            date = this.date.plus(((Interval.YM) interval).toPeriod());
        } else {
            date = this.date.plus(((Interval.DT) interval).toDuration());
        }
        return new Date(date);
    }

    /**
     * @return a new Date representing the result of subtracting the interval to this Date
     */
    @NotNull
    public Date minus(Interval interval) {
        LocalDate date;
        if (interval instanceof Interval.YM) {
            date = this.date.minus(((Interval.YM) interval).toPeriod());
        } else {
            date = this.date.minus(((Interval.DT) interval).toDuration());
        }
        return new Date(date);
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }

    /**
     * In SQL, datetime values are mutually comparable if they share the same fields.
     * <br>
     *
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Date))
            return false;
        return date.equals(((Date) obj).date);
    }

    /**
     * @return the SQL string for this date.
     */
    @Override
    public String toString() {
        return "DATE " + date.format(formatter);
    }
}
