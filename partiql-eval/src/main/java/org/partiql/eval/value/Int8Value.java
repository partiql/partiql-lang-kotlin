package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class Int8Value implements PQLValue {

    private final byte _value;

    Int8Value(byte value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public byte getByte() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.BYTE;
    }
}
