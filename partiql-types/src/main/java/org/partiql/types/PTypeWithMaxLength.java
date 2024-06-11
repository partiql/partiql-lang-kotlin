package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class PTypeWithMaxLength implements PType {

    final int _maxLength;

    final Kind _kind;

    PTypeWithMaxLength(@NotNull Kind type, int maxLength) {
        _kind = type;
        _maxLength = maxLength;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return _kind;
    }

    @Override
    public int getMaxLength() {
        return _maxLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return _kind == ((PType) o).getKind() && _maxLength == ((PType) o).getMaxLength();
    }

    @Override
    public String toString() {
        return _kind.name() + "(" + _maxLength + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(_kind, _maxLength);
    }
}
