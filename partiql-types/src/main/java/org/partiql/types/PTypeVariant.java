package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class PTypeVariant implements PType {

    @NotNull
    final String _encoding;

    public PTypeVariant(@NotNull String encoding) {
        this._encoding = encoding;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.VARIANT;
    }

    @Override
    public String getEncoding() throws UnsupportedOperationException {
        return _encoding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PTypeVariant)) return false;
        PTypeVariant that = (PTypeVariant) o;
        return Objects.equals(_encoding, that._encoding);
    }

    @Override
    public int hashCode() {
        int hashcode = 0;
        hashcode += "variant".hashCode();
        hashcode += 31 * _encoding.hashCode();
        return hashcode;
    }

    @Override
    public String toString() {
        return "VARIANT(" + _encoding + ")";
    }
}
