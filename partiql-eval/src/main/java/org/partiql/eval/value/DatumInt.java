package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumInt implements Datum {

    private final int _value;
    DatumInt(int value) {
        _value = value;
    }

    @Override
    public int getInt() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.INT32;
    }
}
