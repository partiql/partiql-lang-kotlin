package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumString implements Datum {

    @NotNull
    private final String _value;

    @NotNull
    private final PType _type;

    DatumString(@NotNull String value, @NotNull PType type) {
        _value = value;
        _type = type;
    }

    @Override
    @NotNull
    public String getString() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        return "DatumString{" +
                "_value='" + _value + '\'' +
                ", _type=" + _type +
                '}';
    }
}
