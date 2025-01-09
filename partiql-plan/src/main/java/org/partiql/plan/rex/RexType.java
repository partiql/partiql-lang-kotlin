package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * [RexType] is a simple wrapper over [PType], but does not necessarily only hold a PType.
 * <p>
 * <p>
 * Developer Note: In later releases, a [RexType] may hold metadata to aid custom planner implementations.
 */
public final class RexType {

    private final PType type;

    private RexType(PType type) {
        this.type = type;
    }

    /**
     * @return a RexType from a PType.
     */
    @NotNull
    public static RexType of(@NotNull PType type) {
        return new RexType(type);
    }

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
