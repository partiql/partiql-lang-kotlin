package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents different datetime fields in PartiQL.
 *
 * @see org.partiql.ast.expr.ExprExtract
 */
@EqualsAndHashCode(callSuper = false)
public final class DatetimeField extends AstEnum {
    /**
     * Year date time field.
     */
    public static final int YEAR = 0;
    /**
     * Month date time field.
     */
    public static final int MONTH = 1;
    /**
     * Day date time field.
     */
    public static final int DAY = 2;
    /**
     * Hour date time field.
     */
    public static final int HOUR = 3;
    /**
     * Minute date time field.
     */
    public static final int MINUTE = 4;
    /**
     * Second date time field.
     */
    public static final int SECOND = 5;
    /**
     * Timezone hour date time field.
     */
    public static final int TIMEZONE_HOUR = 6;
    /**
     * Timezone minute date time field.
     */
    public static final int TIMEZONE_MINUTE = 7;

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

    @NotNull
    @Override
    public String name() {
        switch (code) {
            case YEAR: return "YEAR";
            case MONTH: return "MONTH";
            case DAY: return "DAY";
            case HOUR: return "HOUR";
            case MINUTE: return "MINUTE";
            case SECOND: return "SECOND";
            case TIMEZONE_HOUR: return "TIMEZONE_HOUR";
            case TIMEZONE_MINUTE: return "TIMEZONE_MINUTE";
            default: throw new IllegalStateException("Invalid DatetimeField code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND,
        TIMEZONE_HOUR,
        TIMEZONE_MINUTE
    };

    @NotNull
    public static DatetimeField parse(@NotNull String value) {
        switch (value) {
            case "YEAR": return YEAR();
            case "MONTH": return MONTH();
            case "DAY": return DAY();
            case "HOUR": return HOUR();
            case "MINUTE": return MINUTE();
            case "SECOND": return SECOND();
            case "TIMEZONE_HOUR": return TIMEZONE_HOUR();
            case "TIMEZONE_MINUTE": return TIMEZONE_MINUTE();
            default: throw new IllegalArgumentException("No enum constant DatetimeField." + value);
        }
    }

    @NotNull
    public static int[] codes() {
        return codes;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return null;
    }
}
