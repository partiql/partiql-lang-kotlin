package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * <p>
 * This is a simple wrapper over {@link PType}, but does not necessarily only hold a {@link PType}.
 * </p>
 * <p>
 * Developer Note: In future releases, this may hold metadata to aid custom planner implementations.
 * </p>
 */
public final class RexType {

    private final PType type;

    private RexType(PType type) {
        this.type = type;
    }

    /**
     * Creates a RexType from a {@link PType}.
     * @param type a {@link PType}.
     * @return a RexType from a {@link PType}.
     */
    @NotNull
    public static RexType of(@NotNull PType type) {
        return new RexType(type);
    }

    /**
     * Gets the underlying {@link PType}.
     * @return the underlying {@link PType}.
     */
    @NotNull
    public PType getPType() {
        return type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RexType)) {
            return false;
        }
        return type.equals(((RexType) obj).type);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
