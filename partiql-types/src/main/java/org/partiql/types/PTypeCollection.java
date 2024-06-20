package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class PTypeCollection implements PType {

    @NotNull
    final PType _typeParam;

    @NotNull
    final Kind _kind;

    PTypeCollection(@NotNull Kind base, @NotNull PType typeParam) {
        _kind = base;
        _typeParam = typeParam;
    }

    @NotNull
    @Override
    public PType getTypeParameter() {
        return _typeParam;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return _kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return ((PType) o).getKind() == this._kind && ((PType) o).getTypeParameter().equals(_typeParam);
    }

    @Override
    public String toString() {
        return _kind.name() + "(" + _typeParam + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(_kind, _typeParam);
    }
}
