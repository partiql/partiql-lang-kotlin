package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class DatetimeField implements Enum {
    public static final int UNKNOWN = 0;
    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOUR = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;
    public static final int TIMEZONE_HOUR = 7;
    public static final int TIMEZONE_MINUTE = 8;

    public static DatetimeField UNKNOWN() {
        return new DatetimeField(UNKNOWN);
    }

    public static DatetimeField YEAR() {
        return new DatetimeField(YEAR);
    }

    public static DatetimeField MONTH() {
        return new DatetimeField(MONTH);
    }

    public static DatetimeField DAY() {
        return new DatetimeField(DAY);
    }

    public static DatetimeField HOUR() {
        return new DatetimeField(HOUR);
    }

    public static DatetimeField MINUTE() {
        return new DatetimeField(MINUTE);
    }

    public static DatetimeField SECOND() {
        return new DatetimeField(SECOND);
    }

    public static DatetimeField TIMEZONE_HOUR() {
        return new DatetimeField(TIMEZONE_HOUR);
    }

    public static DatetimeField TIMEZONE_MINUTE() {
        return new DatetimeField(TIMEZONE_MINUTE);
    }

    private final int code;

    private DatetimeField(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    public static DatetimeField valueOf(String value) {
        switch (value) {
            case "YEAR": return YEAR();
            case "MONTH": return MONTH();
            case "DAY": return DAY();
            case "HOUR": return HOUR();
            case "MINUTE": return MINUTE();
            case "SECOND": return SECOND();
            case "TIMEZONE_HOUR": return TIMEZONE_HOUR();
            case "TIMEZONE_MINUTE": return TIMEZONE_MINUTE();
            default: return UNKNOWN();
        }
    }

    public static DatetimeField[] values() {
        return new DatetimeField[] {
            YEAR(),
            MONTH(),
            DAY(),
            HOUR(),
            MINUTE(),
            SECOND(),
            TIMEZONE_HOUR(),
            TIMEZONE_MINUTE()
        };
    }
}
