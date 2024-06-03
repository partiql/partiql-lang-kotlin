package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class Float32Value implements PQLValue {

    private final float _value;

    Float32Value(float value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public float getFloat() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.FLOAT32;
    }
}
