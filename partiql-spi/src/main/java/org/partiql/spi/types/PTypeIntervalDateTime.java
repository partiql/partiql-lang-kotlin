package org.partiql.spi.types;

import java.util.Objects;

class PTypeIntervalDateTime extends PType {

    private final int code;
    private final int precision;
    private final int fractionalPrecision;

    PTypeIntervalDateTime(int code, int precision, int fractionalPrecision) {
        super(PType.INTERVAL_DT);
        this.code = code;
        this.precision = precision;
        this.fractionalPrecision = fractionalPrecision;
    }

    PTypeIntervalDateTime(int code, int precision) {
        super(PType.INTERVAL_DT);
        this.code = code;
        this.precision = precision;
        this.fractionalPrecision = 0;
    }


    @Override
    public int getIntervalCode() throws UnsupportedOperationException {
        return code;
    }

    @Override
    public int getPrecision() throws UnsupportedOperationException {
        return precision;
    }

    @Override
    public int getFractionalPrecision() throws UnsupportedOperationException {
        return fractionalPrecision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return ((PType) o).code() == code() && ((PType) o).getIntervalCode() == getIntervalCode() && ((PType) o).getPrecision() == getPrecision() && ((PType) o).getFractionalPrecision() == getFractionalPrecision();
    }

    @Override
    public String toString() {
        switch (code) {
            case IntervalCode.DAY:
                return "DAY (" + getPrecision() + ")";
            case IntervalCode.HOUR:
                return "HOUR (" + getPrecision() + ")";
            case IntervalCode.MINUTE:
                return "MINUTE (" + getPrecision() + ")";
            case IntervalCode.SECOND:
                return "SECOND (" + getPrecision() + ", " + getFractionalPrecision() + ")";
            case IntervalCode.DAY_HOUR:
                return "DAY (" + getPrecision() + ") TO HOUR";
            case IntervalCode.DAY_MINUTE:
                return "DAY (" + getPrecision() + ") TO MINUTE";
            case IntervalCode.DAY_SECOND:
                return "DAY (" + getPrecision() + ") TO SECOND (" + getFractionalPrecision() + ")";
            case IntervalCode.HOUR_MINUTE:
                return "HOUR (" + getPrecision() + ") TO MINUTE";
            case IntervalCode.HOUR_SECOND:
                return "HOUR (" + getPrecision() + ") TO SECOND (" + getFractionalPrecision() + ")";
            case IntervalCode.MINUTE_SECOND:
                return "MINUTE (" + getPrecision() + ") TO SECOND (" + getFractionalPrecision() + ")";
            default:
                throw new UnsupportedOperationException("Code " + this.getClass().getName() + "." + code + " does not support toString().");
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(PType.INTERVAL_DT, getIntervalCode(), getPrecision(), getFractionalPrecision());
    }
}
