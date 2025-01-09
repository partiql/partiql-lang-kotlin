package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PType.Kind#TINYINT}
 */
class DatumByte implements Datum {

    private final byte _value;

    @NotNull
    private final PType _type;

    DatumByte(byte value, @NotNull PType type) {
        _value = value;
        _type = type;
    }

    @Override
    public byte getByte() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}
