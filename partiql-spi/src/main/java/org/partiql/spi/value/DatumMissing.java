package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumMissing implements Datum {

    @NotNull
    private final PType _type;

    DatumMissing() {
        _type = PType.unknown();
    }

    DatumMissing(@NotNull PType type) {
        _type = type;
    }

    @Override
    public boolean isMissing() {
        return true;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        return "DatumMissing{" +
                "_type=" + _type +
                '}';
    }
}
