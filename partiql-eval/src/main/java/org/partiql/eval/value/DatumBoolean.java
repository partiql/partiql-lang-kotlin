package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumBoolean implements Datum {

    private final boolean _value;

    DatumBoolean(boolean value) {
        _value = value;
    }

    @Override
    public boolean getBoolean() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.BOOL;
    }
}
