package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class PTypeDecimal implements PType {
    final int _precision;
    final int _scale;

    PTypeDecimal(int precision, int scale) {
        _precision = precision;
        _scale = scale;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.DECIMAL;
    }

    @Override
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
        return ((PType) o).getKind() == Kind.DECIMAL && _precision == ((PType) o).getPrecision() && _scale == ((PType) o).getScale();
    }

    @Override
    public String toString() {
        return Kind.DECIMAL.name() + "(" + _precision + ", " + _scale + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(Kind.DECIMAL, _precision, _scale);
    }
}
