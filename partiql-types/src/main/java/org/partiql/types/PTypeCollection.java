package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class PTypeCollection extends PType {

    @NotNull
    final PType _typeParam;

    PTypeCollection(int code, @NotNull PType typeParam) {
        super(code);
        _typeParam = typeParam;
    }

    @NotNull
    @Override
    public PType getTypeParameter() {
        return _typeParam;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return ((PType) o).code() == this.code() && ((PType) o).getTypeParameter().equals(_typeParam);
    }

    @Override
    public String toString() {
        return name() + "(" + _typeParam + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(code(), _typeParam);
    }
}
