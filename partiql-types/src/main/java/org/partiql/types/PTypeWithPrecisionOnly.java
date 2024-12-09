package org.partiql.types;

import java.util.Objects;

class PTypeWithPrecisionOnly extends PType {

    final int _precision;

    PTypeWithPrecisionOnly(int code, int precision) {
        super(code);
        _precision = precision;
    }

    public int getPrecision() {
        return _precision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return code() == ((PType) o).code() && _precision == ((PType) o).getPrecision();
    }

    @Override
    public String toString() {
        return name() + "(" + _precision + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(code(), _precision);
    }
}
