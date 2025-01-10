package org.partiql.spi.types;

import java.util.Objects;

class PTypePrimitive extends PType {

    PTypePrimitive(int code) {
        super(code);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        return code() == ((PType) o).code();
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code());
    }
}
