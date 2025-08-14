package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.IntervalCode;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal). This class does NOT normalize any of the interval's values. We
 * expect that all values have been normalized and checked already. This also lazily creates its type.
 */
class DatumIntervalDayTime implements Datum {

    private final int days;
    private final int hours;
    private final int minutes;
    private final int seconds;
    private final int nanos;
    private final int precision;
    private final int fractionalPrecision;
    private final int intervalCode;

    // Lazily created type
    private PType _type;

    DatumIntervalDayTime(int days, int hours, int minutes, int seconds, int nanos, int precision, int fractionalPrecision, int intervalCode) {
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.nanos = coerceNanos(nanos, fractionalPrecision);
        this.precision = precision;
        this.fractionalPrecision = fractionalPrecision;
        this.intervalCode = intervalCode;
    }

    /**
     * <p>
     * This removes trailing digits after the fractional precision.
     * </p>
     * <p>
     * For example, consider the interval string "0.123456789". If fractionalPrecision is 2, then 123456789 nanos
     * becomes 120000000 nanos. From the user's perspective, the interval is now "0.12".
     * </p>
     * @param nanos the nanos to coerce
     * @param fractionalPrecision the fractional precision
     * @return the coerced nanos
     */
    private static int coerceNanos(int nanos, int fractionalPrecision) {
        int change = (int) Math.pow(10, 9 - fractionalPrecision);
        return (nanos / change) * change;
    }

    @NotNull
    @Override
    public PType getType() {
        if (this._type == null) {
            initType();
        }
        return this._type;
    }

    private void initType() {
        switch (intervalCode) {
            case IntervalCode.DAY:
                this._type = PType.intervalDay(precision);
                break;
            case IntervalCode.HOUR:
                this._type = PType.intervalHour(precision);
                break;
            case IntervalCode.MINUTE:
                this._type = PType.intervalMinute(precision);
                break;
            case IntervalCode.SECOND:
                this._type = PType.intervalSecond(precision, fractionalPrecision);
                break;
            case IntervalCode.DAY_HOUR:
                this._type = PType.intervalDayHour(precision);
                break;
            case IntervalCode.DAY_MINUTE:
                this._type = PType.intervalDayMinute(precision);
                break;
            case IntervalCode.DAY_SECOND:
                this._type = PType.intervalDaySecond(precision, fractionalPrecision);
                break;
            case IntervalCode.HOUR_MINUTE:
                this._type = PType.intervalHourMinute(precision);
                break;
            case IntervalCode.HOUR_SECOND:
                this._type = PType.intervalHourSecond(precision, fractionalPrecision);
                break;
            case IntervalCode.MINUTE_SECOND:
                this._type = PType.intervalMinuteSecond(precision, fractionalPrecision);
                break;
            default:
                throw new IllegalStateException("Unknown interval code for day-time: " + intervalCode);
        }
    }

    @Override
    public int getDays() throws InvalidOperationException, NullPointerException {
        return days;
    }

    @Override
    public int getHours() {
        return hours;
    }

    @Override
    public int getMinutes() throws InvalidOperationException, NullPointerException {
        return minutes;
    }

    @Override
    public int getSeconds() throws InvalidOperationException, NullPointerException {
        return this.seconds;
    }

    @Override
    public int getNanos() {
        return this.nanos;
    }

    @Override
    public String toString() {
        return "DatumIntervalDayTime{" +
                "_type=" + getType() +
                ", _value=" + "INTERVAL '" + days + " " + hours + ":" + minutes + ":" + seconds + "." + nanos + "'" +
                '}';
    }
}
