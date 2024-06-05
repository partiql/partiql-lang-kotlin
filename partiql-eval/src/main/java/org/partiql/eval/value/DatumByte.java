package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PartiQLValueType#BYTE},
 * {@link PartiQLValueType#INT8}
 */
class DatumByte implements Datum {

    private final byte _value;

    @NotNull
    private final PartiQLValueType _type;

    DatumByte(byte value, @NotNull PartiQLValueType type) {
        assert(type == PartiQLValueType.BYTE || type == PartiQLValueType.INT8);
        _value = value;
        _type = type;
    }

    @Override
    public byte getByte() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return _type;
    }
}
