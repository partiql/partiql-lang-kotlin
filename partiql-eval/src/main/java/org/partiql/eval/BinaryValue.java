package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class BinaryValue implements PQLValue {

    @NotNull
    final byte[] _value;

    BinaryValue(@NotNull byte[] value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public byte[] getBinaryValue() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.BINARY;
    }
}
