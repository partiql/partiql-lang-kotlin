package org.partiql.spi;

import java.util.Objects;

/**
 * Represents an enum in the PartiQL Library. This is used for backward/forward compatibility purposes.
 */
public abstract class PEnum {
    private final int code;

    /**
     * Creates a {@link PEnum} with the specified {@code code}.
     * @param code the unique code of this enum.
     */
    protected PEnum(int code) {
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
        if (!(o instanceof PEnum)) return false;
        PEnum pEnum = (PEnum) o;
        return code == pEnum.code;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
