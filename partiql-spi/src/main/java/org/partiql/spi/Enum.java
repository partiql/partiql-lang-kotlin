package org.partiql.spi;

import org.jetbrains.annotations.NotNull;

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

    /**
     * @return the name of the enum variant.
     * @throws UnsupportedCodeException if the code is not recognized by this enum.
     */
    @NotNull
    public abstract String name() throws UnsupportedCodeException;

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

    @Override
    public String toString() {
        try {
            return name();
        } catch (UnsupportedCodeException e) {
            return String.valueOf(code);
        }
    }
}
