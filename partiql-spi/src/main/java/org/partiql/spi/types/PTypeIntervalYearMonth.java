package org.partiql.spi.types;

import java.util.Objects;

class PTypeIntervalYearMonth extends PType {
    private final int code;
    private final int precision;

    PTypeIntervalYearMonth(int code, int precision) {
        super(PType.INTERVAL_YM);
        this.code = code;
        this.precision = precision;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return ((PType) o).code() == code() && ((PType) o).getIntervalCode() == getIntervalCode() && ((PType) o).getPrecision() == getPrecision();
    }

    @Override
    public String toString() {
        switch (code) {
            case IntervalCode.YEAR:
                return "YEAR (" + getPrecision() + ")";
            case IntervalCode.MONTH:
                return "MONTH (" + getPrecision() + ")";
            case IntervalCode.YEAR_MONTH:
                return "YEAR (" + getPrecision() + ") TO MONTH";
            default:
                throw new UnsupportedOperationException("Code " + this.getClass().getName() + "." + code + " does not support toString().");
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(PType.INTERVAL_YM, getIntervalCode(), getPrecision());
    }
}
