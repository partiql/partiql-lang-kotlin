package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class PTypePrimitive implements PType {

    @NotNull
    final Kind _kind;

    PTypePrimitive(@NotNull Kind type) {
        _kind = type;
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
        return _kind == ((PType) o).getKind();
    }

    @Override
    public String toString() {
        return _kind.name();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_kind);
    }
}
