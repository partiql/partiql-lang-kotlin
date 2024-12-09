package org.partiql.types;

import java.util.Objects;

/**
 * Relevant to only {@link PType#DECIMAL} and {@link PType#NUMERIC}.
 */
class PTypeDecimal extends PType {
    final int _precision;
    final int _scale;

    PTypeDecimal(int code, int precision, int scale) {
        super(code);
        _precision = precision;
        _scale = scale;
    }

    public int getPrecision() {
        return _precision;
    }

    @Override
    public int getScale() {
        return _scale;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return ((PType) o).code() == code() && _precision == ((PType) o).getPrecision() && _scale == ((PType) o).getScale();
    }

    @Override
    public String toString() {
        return name() + "(" + _precision + ", " + _scale + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(PType.DECIMAL, _precision, _scale);
    }
}
