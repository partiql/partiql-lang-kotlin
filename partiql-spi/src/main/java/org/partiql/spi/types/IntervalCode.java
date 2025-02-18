package org.partiql.spi.types;

/**
 * <p>
 * According to SQL:1999, an interval data type descriptor is used to provide information such as the precision and
 * day-time/year-month classification for a particular interval value. The classification of a particular interval value
 * is represented as integers and may be referenced using the static final integers under this class.
 * </p>
 * @see PType#INTERVAL_YM
 * @see PType#INTERVAL_DT
 */
public final class IntervalCode {
    // In the future, this may extend org.partiql.spi.Enum if we want access to codes(), name(), etc.
    // Right now, it is not required.

    public static final int YEAR = 0;
    public static final int MONTH = 1;
    public static final int DAY = 2;
    public static final int HOUR = 3;
    public static final int MINUTE = 4;
    public static final int SECOND = 5;
    public static final int YEAR_MONTH = 6;
    public static final int DAY_HOUR = 7;
    public static final int DAY_MINUTE = 8;
    public static final int DAY_SECOND = 9;
    public static final int HOUR_MINUTE = 10;
    public static final int HOUR_SECOND = 11;
    public static final int MINUTE_SECOND = 12;

    private IntervalCode() {
        // This is only so that no one can instantiate this class.
    }
}
