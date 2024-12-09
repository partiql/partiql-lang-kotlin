package org.partiql.spi;

import java.util.Objects;

/**
 * All enumerated types should extend this class for backward/forward compatibility.
 */
public abstract class Enum {

    /**
     * Enum variants are represented with integers.
     */
    private final int code;

    /**
     * Creates an {@link Enum} with the specified {@code code}.
     * @param code the unique code of this enum.
     */
    protected Enum(int code) {
        this.code = code;
    }

    /**
     * @return a unique integer corresponding with the variant of the enum.
     */
    public final int code() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Enum)) return false;
        Enum other = (Enum) o;
        return code == other.code;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
