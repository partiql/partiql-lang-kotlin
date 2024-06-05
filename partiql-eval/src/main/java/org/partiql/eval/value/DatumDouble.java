package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumDouble implements Datum {

    private final double _value;

    DatumDouble(double value) {
        _value = value;
    }

    @Override
    public double getDouble() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.FLOAT64;
    }
}
