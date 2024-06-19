package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PType.Kind#BLOB},
 * {@link PType.Kind#CLOB}
 */
class DatumBytes implements Datum {

    @NotNull
    private final byte[] _value;

    @NotNull
    private final PType _type;

    DatumBytes(@NotNull byte[] value, @NotNull PType type) {
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
    public PType getType() {
        return _type;
    }
}
