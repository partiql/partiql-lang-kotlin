package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PartiQLValueType#BINARY},
 * {@link PartiQLValueType#BLOB},
 * {@link PartiQLValueType#CLOB}
 */
class DatumBytes implements Datum {

    @NotNull
    private final byte[] _value;

    @NotNull
    private final PartiQLValueType _type;

    DatumBytes(@NotNull byte[] value, @NotNull PartiQLValueType type) {
        assert(type == PartiQLValueType.BINARY || type == PartiQLValueType.BLOB || type == PartiQLValueType.CLOB);
        _value = value;
        _type = type;
    }

    @Override
    @NotNull
    public byte[] getBytes() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return _type;
    }
}
