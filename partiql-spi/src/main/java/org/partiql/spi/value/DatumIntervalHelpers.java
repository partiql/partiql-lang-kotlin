package org.partiql.spi.value;

/**
 * PACKAGE-PRIVATE.
 */
class DatumIntervalHelpers {

    static void checkPrecision(int precision) {
        if (precision < 0 || precision > 9) {
            throw new IllegalArgumentException("Leading field precision must be between 0 and 9 inclusive");
        }
    }

    static void checkScale(int scale) {
        if (scale < 0 || scale > 9) {
            throw new IllegalArgumentException("Fractional seconds precision must be between 0 and 6 inclusive");
        }
    }

    static void checkUsingPrecision(int value, int precision) {
        long newValue = Math.abs((long) value);
        if (newValue < -Math.pow(10, precision) || newValue > Math.pow(10, precision) - 1) {
            throw new IllegalArgumentException("Value " + value + " is out of range for precision " + precision);
        }
    }

    static void checkHours(int hours) {
        long newHours = Math.abs((long) hours);
        if (newHours > 23) {
            throw new IllegalArgumentException("Hours must be between 0 and 23 inclusive");
        }
    }

    static void checkMinutes(int minutes) {
        long newMinutes = Math.abs((long) minutes);
        if (newMinutes > 59) {
            throw new IllegalArgumentException("Minutes must be between 0 and 59 inclusive");
        }
    }

    static void checkSeconds(int seconds) {
        long newSeconds = Math.abs((long) seconds);
        if (newSeconds > 59) {
            throw new IllegalArgumentException("Seconds must be between 0 and 59 inclusive");
        }
    }

    static void checkMonths(int months) {
        long newMonths = Math.abs((long) months);
        if (newMonths > 11) {
            throw new IllegalArgumentException("Months must be between 0 and 11 inclusive");
        }
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
    static int coerceNanos(int nanos, int fractionalPrecision) {
        int change = (int) Math.pow(10, 9 - fractionalPrecision);
        return (nanos / change) * change;
    }
}
