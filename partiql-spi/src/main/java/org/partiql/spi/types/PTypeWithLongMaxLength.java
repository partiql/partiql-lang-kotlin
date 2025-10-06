package org.partiql.spi.types;

import java.util.Objects;

class PTypeWithLongMaxLength extends PType {

    final long _maxLength;

    PTypeWithLongMaxLength(int code, long maxLength) {
        super(code);
        _maxLength = maxLength;
    }

    @Override
    public int getLength() {
        return (int) _maxLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return code() == ((PType) o).code() && _maxLength == ((PType) o).getLength();
    }

    @Override
    public String toString() {
        return name() + "(" + _maxLength + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(code(), _maxLength);
    }
}