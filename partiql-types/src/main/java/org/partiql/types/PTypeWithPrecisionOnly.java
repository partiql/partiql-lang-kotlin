package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class PTypeWithPrecisionOnly implements PType {

    final int _precision;

    @NotNull
    final Kind _kind;

    PTypeWithPrecisionOnly(@NotNull Kind base, int precision) {
        _precision = precision;
        _kind = base;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return _kind;
    }

    @Override
    public int getPrecision() {
        return _precision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return _kind == ((PType) o).getKind() && _precision == ((PType) o).getPrecision();
    }

    @Override
    public String toString() {
        return _kind.name() + "(" + _precision + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(_kind, _precision);
    }
}
