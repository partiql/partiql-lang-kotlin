package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumChars implements Datum {

    @NotNull
    private final String _value;

    @NotNull
    private final PType _type;

    DatumChars(@NotNull String value, int length) {
        _value = value;
        _type = PType.character(length);
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
}
