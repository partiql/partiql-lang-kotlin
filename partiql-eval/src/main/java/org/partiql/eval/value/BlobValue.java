package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class BlobValue implements PQLValue {

    @NotNull
    private final byte[] _value;

    BlobValue(@NotNull byte[] value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public byte[] getBytes() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.BLOB;
    }
}
