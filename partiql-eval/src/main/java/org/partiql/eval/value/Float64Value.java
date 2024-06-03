package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class Float64Value implements PQLValue {

    private final double _value;

    Float64Value(double value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
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
