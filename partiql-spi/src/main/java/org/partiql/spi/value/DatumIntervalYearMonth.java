package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.IntervalCode;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal). This class does NOT normalize any of the interval's values. We
 * expect that all values have been normalized and checked already. This also lazily creates its type.
 */
class DatumIntervalYearMonth implements Datum {

    private final int _years;
    private final int _months;
    private final int precision;
    private final int intervalCode;
    private PType _type;

    DatumIntervalYearMonth(int years, int months, int precision, int intervalCode) {
        _years = years;
        _months = months;
        this.precision = precision;
        this.intervalCode = intervalCode;
    }

    @NotNull
    @Override
    public PType getType() {
        if (_type == null) {
            initType();
        }
        return _type;
    }

    private void initType() {
        switch (intervalCode) {
            case IntervalCode.YEAR:
                this._type = PType.intervalYear(precision);
                break;
            case IntervalCode.MONTH:
                this._type = PType.intervalMonth(precision);
                break;
            case IntervalCode.YEAR_MONTH:
                this._type = PType.intervalYearMonth(precision);
                break;
            default:
                throw new IllegalStateException("Unknown interval code for year-month: " + intervalCode);
        }
    }

    @Override
    public int getYears() throws InvalidOperationException, NullPointerException {
        return _years;
    }

    @Override
    public int getMonths() throws InvalidOperationException, NullPointerException {
        return _months;
    }

    @Override
    public String toString() {
        return "DatumIntervalYearMonth{" +
                "_years=" + _years +
                ", _months=" + _months +
                ", precision=" + precision +
                ", intervalCode=" + intervalCode +
                ", _type=" + _type +
                '}';
    }
}
