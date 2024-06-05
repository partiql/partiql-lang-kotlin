package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumFloat implements Datum {

    private final float _value;

    DatumFloat(float value) {
        _value = value;
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
